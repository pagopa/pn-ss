package it.pagopa.pnss.uribuilder;

import it.pagopa.pn.template.internal.rest.v1.dto.*;
import it.pagopa.pn.template.rest.v1.dto.UpdateFileMetadataRequest;
import it.pagopa.pnss.common.client.DocTypesClientCall;
import it.pagopa.pnss.common.client.DocumentClientCall;
import it.pagopa.pnss.common.client.UserConfigurationClientCall;
import it.pagopa.pnss.common.client.exception.DocumentKeyNotPresentException;
import it.pagopa.pnss.testutils.annotation.SpringBootTestWebEnv;
import it.pagopa.pnss.uribuilder.service.UriBuilderService;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Arrays;
import java.util.Map;

import static it.pagopa.pnss.common.Constant.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@SpringBootTestWebEnv
@AutoConfigureWebTestClient
@Slf4j
class FileMetadataUpdateApiControllerTest {

    @Autowired
    private WebTestClient webClient;

    @Autowired
    private UriBuilderService uriBuilderService;

    @MockBean
    UserConfigurationClientCall userConfigurationClientCall;

    @MockBean
    DocumentClientCall documentClientCall;

    @MockBean
    DocTypesClientCall docTypesClientCall;

    @Value("${header.x-api-key:#{null}}")
    private String xApiKey;

    @Value("${header.x-pagopa-safestorage-cx-id:#{null}}")
    private String X_PAGOPA_SAFESTORAGE_CX_ID;

    @Value("${file.updateMetadata.api.url}")
    private String urlPath;

    private static final String X_PAGO_PA_SAFESTORAGE_CX_ID_VALUE = "CLIENT_ID_123";
    private static final String xApiKeyValue = "apiKey_value";
    private static final String xPagoPaSafestorageCxIdValue = "CLIENT_ID_123";
    private static final DocumentResponse DOCUMENT_RESPONSE = new DocumentResponse().document(new Document().documentKey("documentKey"));
    private static final String X_API_KEY_VALUE = "apiKey_value";

    private WebTestClient.ResponseSpec fileMetadataUpdateTestCall(UpdateFileMetadataRequest updateFileMetadataRequest, String documentKey) {

        webClient.mutate().responseTimeout(Duration.ofMillis(30000)).build();

        return webClient.post()
                        .uri(uriBuilder -> uriBuilder.path(urlPath).queryParam("metadataOnly", false).build(documentKey))
                        .header(X_PAGOPA_SAFESTORAGE_CX_ID, xPagoPaSafestorageCxIdValue)
                        .header(xApiKey, X_API_KEY_VALUE)
                        .header(HttpHeaders.ACCEPT, APPLICATION_JSON_VALUE)
                        .bodyValue(updateFileMetadataRequest)
                        .exchange();
    }

    @BeforeEach
    public void createUserConfiguration() {
        var userConfiguration = new UserConfiguration().name(X_PAGO_PA_SAFESTORAGE_CX_ID_VALUE).apiKey(X_API_KEY_VALUE);
        var userConfigurationResponse = new UserConfigurationResponse().userConfiguration(userConfiguration);
        when(userConfigurationClientCall.getUser(anyString())).thenReturn(Mono.just(userConfigurationResponse));
    }

    @Test
    void testDocumentKeyNotPresent() {
        when(documentClientCall.getdocument(anyString())).thenReturn(Mono.error(new DocumentKeyNotPresentException("keyFile")));
        fileMetadataUpdateTestCall(new UpdateFileMetadataRequest(), X_PAGOPA_SAFESTORAGE_CX_ID).expectStatus().isNotFound();
    }

    @Test
    void testErrorStatus() {
        UpdateFileMetadataRequest req = new UpdateFileMetadataRequest();
        req.setStatus(PRELOADED);
        DocumentResponse resp = new DocumentResponse();
        Document document = new Document();
        DocumentType documentType = new DocumentType();
        documentType.setStatuses(Map.ofEntries(Map.entry(PRELOADED, new CurrentStatus())));
        documentType.setTipoDocumento(PN_AAR);
        document.setDocumentType(documentType);
        resp.setDocument(document);
        Mono<DocumentResponse> monoResp = Mono.just(resp);
        Mono<UserConfigurationResponse> userConfigurationResponse = mockUserConfiguration();
        Mockito.doReturn(userConfigurationResponse).when(userConfigurationClientCall).getUser(Mockito.any());
        Mockito.doReturn(monoResp).when(documentClientCall).getdocument(Mockito.any());

        CurrentStatus _cs = new CurrentStatus();
        _cs.setTechnicalState("available");
        DocumentType _documentType = new DocumentType();
        _documentType.setStatuses(Map.ofEntries(Map.entry(PRELOADED, _cs)));
        _documentType.setTipoDocumento(PN_AAR);
        DocumentTypeResponse _docTypeResp = new DocumentTypeResponse();
        _docTypeResp.setDocType(_documentType);
        Mono<DocumentTypeResponse> _monoDocTypeResp = Mono.just(_docTypeResp);
        Mockito.doReturn(_monoDocTypeResp).when(docTypesClientCall).getdocTypes(Mockito.any());

        fileMetadataUpdateTestCall(BodyInserters.fromValue(req), X_PAGOPA_SAFESTORAGE_CX_ID).expectStatus().isBadRequest();
    }

    @Test
    void testErrorTechnicalStatus() throws Exception {
        UpdateFileMetadataRequest req = new UpdateFileMetadataRequest();
        req.setStatus(ATTACHED);
        DocumentResponse resp = new DocumentResponse();
        Document document = new Document();
        DocumentType documentType = new DocumentType();
        documentType.setStatuses(Map.ofEntries(Map.entry(PRELOADED, new CurrentStatus())));
        documentType.setTipoDocumento(PN_AAR);
        document.setDocumentType(documentType);
        resp.setDocument(document);
        Mono<DocumentResponse> monoResp = Mono.just(resp);
        Mono<UserConfigurationResponse> userConfigurationResponse = mockUserConfiguration();
        Mockito.doReturn(userConfigurationResponse).when(userConfigurationClientCall).getUser(Mockito.any());

        Mockito.doReturn(monoResp).when(documentClientCall).getdocument(Mockito.any());

        CurrentStatus _cs = new CurrentStatus();
        _cs.setTechnicalState("available");
        DocumentType _documentType = new DocumentType();
        _documentType.setStatuses(Map.ofEntries(Map.entry(PRELOADED, _cs)));
        _documentType.setTipoDocumento(PN_AAR);
        DocumentTypeResponse _docTypeResp = new DocumentTypeResponse();
        _docTypeResp.setDocType(_documentType);
        Mono<DocumentTypeResponse> _monoDocTypeResp = Mono.just(_docTypeResp);
        Mockito.doReturn(_monoDocTypeResp).when(docTypesClientCall).getdocTypes(Mockito.any());

        fileMetadataUpdateTestCall(BodyInserters.fromValue(req), X_PAGOPA_SAFESTORAGE_CX_ID).expectStatus().isBadRequest();

    }

    @Test
    void testErrorUserCongigurationWithoutPrivileg() throws Exception {
        UpdateFileMetadataRequest req = new UpdateFileMetadataRequest();
        req.setStatus(SAVED);
        DocumentResponse resp = new DocumentResponse();
        Document document = new Document();
        DocumentType documentType = new DocumentType();
        documentType.setStatuses(Map.ofEntries(Map.entry(SAVED, new CurrentStatus())));
        documentType.setTipoDocumento(PN_AAR);
        document.setDocumentType(documentType);
        resp.setDocument(document);
        Mono<DocumentResponse> monoResp = Mono.just(resp);

        Mockito.doReturn(monoResp).when(documentClientCall).getdocument(Mockito.any());

        CurrentStatus _cs = new CurrentStatus();
        _cs.setTechnicalState("available");
        DocumentType _documentType = new DocumentType();
        _documentType.setStatuses(Map.ofEntries(Map.entry(PRELOADED, _cs)));
        _documentType.setTipoDocumento(PN_AAR);
        DocumentTypeResponse _docTypeResp = new DocumentTypeResponse();
        _docTypeResp.setDocType(_documentType);
        Mono<DocumentTypeResponse> _monoDocTypeResp = Mono.just(_docTypeResp);
        Mockito.doReturn(_monoDocTypeResp).when(docTypesClientCall).getdocTypes(Mockito.any());

        WebTestClient.ResponseSpec responseSpec =
                fileMetadataUpdateTestCall(BodyInserters.fromValue(req), X_PAGOPA_SAFESTORAGE_CX_ID).expectStatus().isForbidden();

    }

    @Test
    void testFileMetadataUpgateOk() throws Exception {
        UpdateFileMetadataRequest req = new UpdateFileMetadataRequest();
        req.setStatus(SAVED);
        DocumentResponse resp = new DocumentResponse();
        Document document = new Document();
        DocumentType documentType = new DocumentType();
        documentType.setStatuses(Map.ofEntries(Map.entry(SAVED, new CurrentStatus())));
        documentType.setTipoDocumento(PN_AAR);
        document.setDocumentType(documentType);
        resp.setDocument(document);
        Mono<DocumentResponse> monoResp = Mono.just(resp);

        Mockito.doReturn(monoResp).when(documentClientCall).getdocument(Mockito.any());

        Mono<UserConfigurationResponse> userConfigurationResponse = mockUserConfiguration();
        Mockito.doReturn(userConfigurationResponse).when(userConfigurationClientCall).getUser(Mockito.any());

        Mockito.doReturn(monoResp).when(documentClientCall).patchdocument(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());

        CurrentStatus _cs = new CurrentStatus();
        _cs.setTechnicalState("available");
        DocumentType _documentType = new DocumentType();
        _documentType.setStatuses(Map.ofEntries(Map.entry(PRELOADED, _cs)));
        _documentType.setTipoDocumento(PN_AAR);
        DocumentTypeResponse _docTypeResp = new DocumentTypeResponse();
        _docTypeResp.setDocType(_documentType);
        Mono<DocumentTypeResponse> _monoDocTypeResp = Mono.just(_docTypeResp);
        Mockito.doReturn(_monoDocTypeResp).when(docTypesClientCall).getdocTypes(Mockito.any());

        WebTestClient.ResponseSpec responseSpec =
                fileMetadataUpdateTestCall(BodyInserters.fromValue(req), X_PAGOPA_SAFESTORAGE_CX_ID).expectStatus().isOk();

    }

    @NotNull
    private static Mono<UserConfigurationResponse> mockUserConfiguration() {
        UserConfiguration userConfiguration = new UserConfiguration();
        userConfiguration.setName(X_PAGO_PA_SAFESTORAGE_CX_ID_VALUE);
        userConfiguration.setApiKey(X_API_KEY_VALUE);
        userConfiguration.setCanModifyStatus(Arrays.asList(PN_AAR));
        UserConfigurationResponse userConfig = new UserConfigurationResponse();
        userConfig.setUserConfiguration(userConfiguration);

        Mono<UserConfigurationResponse> userConfigurationResponse = Mono.just(userConfig);
        return userConfigurationResponse;
    }

}
