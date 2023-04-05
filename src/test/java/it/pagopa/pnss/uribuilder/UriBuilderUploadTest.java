package it.pagopa.pnss.uribuilder;

import it.pagopa.pn.template.internal.rest.v1.dto.*;
import it.pagopa.pn.template.rest.v1.dto.FileCreationRequest;
import it.pagopa.pn.template.rest.v1.dto.FileCreationResponse;
import it.pagopa.pnss.common.DocTypesConstant;
import it.pagopa.pnss.common.client.DocTypesClientCall;
import it.pagopa.pnss.common.client.DocumentClientCall;
import it.pagopa.pnss.common.client.UserConfigurationClientCall;
import it.pagopa.pnss.repositorymanager.service.DocTypesService;
import it.pagopa.pnss.testutils.annotation.SpringBootTestWebEnv;
import lombok.extern.slf4j.Slf4j;

import org.junit.jupiter.api.*;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.FluxExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

import static it.pagopa.pnss.common.DocTypesConstant.*;
import static it.pagopa.pnss.common.constant.Constant.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;


@SpringBootTestWebEnv
@AutoConfigureWebTestClient(timeout = "36000")
@Slf4j
class UriBuilderUploadTest {

    @Autowired
    private WebTestClient webClient;

    @MockBean
    private DocTypesClientCall docTypesClientCall;

    @MockBean
    private UserConfigurationClientCall userConfigurationClientCall;

    @MockBean
    private DocTypesService docTypesService;

    @MockBean
    private DocumentClientCall documentClientCall;

    @Value("${header.x-api-key:#{null}}")
    private String xApiKey;

    @Value("${header.x-pagopa-safestorage-cx-id:#{null}}")
    private String X_PAGOPA_SAFESTORAGE_CX_ID;

    @Value("${header.x-checksum-value:#{null}}")
    private String headerChecksumValue;

    @Value("${file.upload.api.url}")
    private String urlPath;

    private static final String xApiKeyValue = "apiKey_value";
    private static final String xPagoPaSafestorageCxIdValue = "CLIENT_ID_123";
    private static final String xChecksumValue = "checkSumValue";
    private static final DocumentResponse DOCUMENT_RESPONSE = new DocumentResponse().document(new Document().documentKey("documentKey"));

    private WebTestClient.ResponseSpec fileUploadTestCall(FileCreationRequest fileCreationRequest) {
        return this.webClient.post()
                             .uri(urlPath)
                             .accept(MediaType.APPLICATION_JSON)
                             .contentType(MediaType.APPLICATION_JSON)
                             .bodyValue(fileCreationRequest)
                             .header(X_PAGOPA_SAFESTORAGE_CX_ID, xPagoPaSafestorageCxIdValue)
                             .header(xApiKey, xApiKeyValue)
                             .header(headerChecksumValue, xChecksumValue)
                             .exchange();
    }

//    @Test
//    void testStatoNonConsentito_PN_NOTIFICATION_ATTACHMENTS() {
//
//        UserConfigurationResponse userConfig = new UserConfigurationResponse();
//        UserConfiguration userConfiguration = new UserConfiguration();
//        userConfiguration.setName(xPagoPaSafestorageCxIdValue);
//        userConfiguration.setApiKey(xApiKeyValue);
//        userConfig.setUserConfiguration(userConfiguration);
//
//        when(userConfigurationClientCall.getUser(anyString())).thenReturn(Mono.just(userConfig));
//        when(documentClientCall.postDocument(any(DocumentInput.class))).thenReturn(Mono.just(DOCUMENT_RESPONSE));
//
//
//        FileCreationRequest fcr = new FileCreationRequest();
//        fcr.setContentType(IMAGE_TIFF);
//        fcr.setDocumentType(PN_NOTIFICATION_ATTACHMENTS);
//        fcr.setStatus(ATTACHED);
//        fileUploadTestCall(BodyInserters.fromValue(fcr), X_PAGOPA_SAFESTORAGE_CX_ID).expectStatus().isBadRequest();
//    }

//    @Test
//    void testStatoNonConsentito_PN_AAR() {
//
//        UserConfigurationResponse userConfig = new UserConfigurationResponse();
//        UserConfiguration userConfiguration = new UserConfiguration();
//        userConfiguration.setName(xPagoPaSafestorageCxIdValue);
//        userConfiguration.setApiKey(xApiKeyValue);
//        userConfig.setUserConfiguration(userConfiguration);
//
//        when(userConfigurationClientCall.getUser(anyString())).thenReturn(Mono.just(userConfig));
//        when(documentClientCall.postDocument(any(DocumentInput.class))).thenReturn(Mono.just(DOCUMENT_RESPONSE));
//
//        FileCreationRequest fcr = new FileCreationRequest();
//        fcr.setContentType(IMAGE_TIFF);
//        fcr.setDocumentType(PN_AAR);
//        fcr.setStatus(PRELOADED);
//        fileUploadTestCall(BodyInserters.fromValue(fcr), X_PAGOPA_SAFESTORAGE_CX_ID).expectStatus().isBadRequest();
//    }


    @Test
    void testErroreInserimentoContentType() {

        UserConfigurationResponse userConfig = new UserConfigurationResponse();
        UserConfiguration userConfiguration = new UserConfiguration();
        userConfiguration.setName(xPagoPaSafestorageCxIdValue);
        userConfiguration.setApiKey(xApiKeyValue);
        userConfig.setUserConfiguration(userConfiguration);

        when(documentClientCall.postDocument(any(DocumentInput.class))).thenReturn(Mono.just(DOCUMENT_RESPONSE));
        when(userConfigurationClientCall.getUser(anyString())).thenReturn(Mono.just(userConfig));

        FileCreationRequest fcr = new FileCreationRequest();
        fcr.setContentType("VALUE_FAULT");
        fcr.setDocumentType(PN_AAR);
        fcr.setStatus(PRELOADED);
        fileUploadTestCall(fcr).expectStatus().isBadRequest();
    }

//    @Test
//    void testErroreInserimentoStatus() {
//
//        UserConfigurationResponse userConfig = new UserConfigurationResponse();
//        UserConfiguration userConfiguration = new UserConfiguration();
//        userConfiguration.setName(xPagoPaSafestorageCxIdValue);
//        userConfiguration.setApiKey(xApiKeyValue);
//        userConfig.setUserConfiguration(userConfiguration);
//
//        when(documentClientCall.postDocument(any(DocumentInput.class))).thenReturn(Mono.just(DOCUMENT_RESPONSE));
//        when(userConfigurationClientCall.getUser(anyString())).thenReturn(Mono.just(userConfig));
//
//        FileCreationRequest fcr = new FileCreationRequest();
//        fcr.setContentType(IMAGE_TIFF);
//        fcr.setDocumentType(PN_AAR);
//        fcr.setStatus("VALUE_FAULT");
//
//        fileUploadTestCall(BodyInserters.fromValue(fcr), X_PAGOPA_SAFESTORAGE_CX_ID).expectStatus().isBadRequest();
//    }

    @Test
    void testContentTypeParamObbligatorio() {

        UserConfigurationResponse userConfig = new UserConfigurationResponse();
        UserConfiguration userConfiguration = new UserConfiguration();
        userConfiguration.setName(xPagoPaSafestorageCxIdValue);
        userConfiguration.setApiKey(xApiKeyValue);
        userConfig.setUserConfiguration(userConfiguration);

        when(documentClientCall.postDocument(any(DocumentInput.class))).thenReturn(Mono.just(DOCUMENT_RESPONSE));
        when(userConfigurationClientCall.getUser(anyString())).thenReturn(Mono.just(userConfig));

        FileCreationRequest fcr = new FileCreationRequest();
        fcr.setDocumentType(PN_AAR);
        fcr.setStatus("VALUE_FAULT");

        fileUploadTestCall(fcr).expectStatus().isBadRequest();
    }

    @Test
    void testDocumentTypeParamObbligatorio() {

        UserConfigurationResponse userConfig = new UserConfigurationResponse();
        UserConfiguration userConfiguration = new UserConfiguration();
        userConfiguration.setName(xPagoPaSafestorageCxIdValue);
        userConfiguration.setApiKey(xApiKeyValue);
        userConfig.setUserConfiguration(userConfiguration);

        when(documentClientCall.postDocument(any(DocumentInput.class))).thenReturn(Mono.just(DOCUMENT_RESPONSE));
        when(userConfigurationClientCall.getUser(anyString())).thenReturn(Mono.just(userConfig));

        FileCreationRequest fcr = new FileCreationRequest();
        fcr.setDocumentType(PN_AAR);
        fcr.setStatus("VALUE_FAULT");

        fileUploadTestCall(fcr).expectStatus().isBadRequest();
    }

    @Test
    void testIdClienteNonTrovatoUpload() {

        var fcr = new FileCreationRequest().contentType(IMAGE_TIFF_VALUE).documentType(PN_AAR).status("");

        when(userConfigurationClientCall.getUser(anyString())).thenReturn(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND,
                                                                                                                 "User Not Found : ")));

        fileUploadTestCall(fcr).expectStatus().isNotFound();
    }

    @Nested
    class ValidationFieldSuccess{

        private static final List<DocumentType> DOCUMENT_TYPE_LIST = List.of(new DocumentType().tipoDocumento(PN_NOTIFICATION_ATTACHMENTS),
                                                                             new DocumentType().tipoDocumento(PN_AAR),
                                                                             new DocumentType().tipoDocumento(PN_LEGAL_FACTS),
                                                                             new DocumentType().tipoDocumento(PN_EXTERNAL_LEGAL_FACTS));

        @BeforeEach
        void setUp() {
            when(docTypesService.getAllDocumentType()).thenReturn(Mono.just(DOCUMENT_TYPE_LIST));
        }

        @Test
        void testUrlGenStatusPre() {

            FileCreationRequest fcr = new FileCreationRequest();
            fcr.setContentType(IMAGE_TIFF_VALUE);
            fcr.setDocumentType(PN_NOTIFICATION_ATTACHMENTS);
            fcr.setStatus(PRELOADED);
            FileCreationResponse fcresp = new FileCreationResponse();
            fcresp.setUploadUrl("http://host:9090/urlFile");

            UserConfigurationResponse userConfig = new UserConfigurationResponse();
            UserConfiguration userConfiguration = new UserConfiguration();
            userConfiguration.setName(xPagoPaSafestorageCxIdValue);
            userConfiguration.setApiKey(xApiKeyValue);
            userConfiguration.setCanCreate(List.of(PN_NOTIFICATION_ATTACHMENTS));
            userConfig.setUserConfiguration(userConfiguration);

            Mono<UserConfigurationResponse> userConfigurationEntity = Mono.just(userConfig);
            Mockito.doReturn(userConfigurationEntity).when(userConfigurationClientCall).getUser(Mockito.any());

            DocumentTypeResponse documentTypeResponse = new DocumentTypeResponse();
            DocumentType documentType = new DocumentType();
            documentType.setTipoDocumento(PN_LEGAL_FACTS);
            documentTypeResponse.setDocType(documentType);

            Mono<DocumentTypeResponse> docTypeEntity = Mono.just(documentTypeResponse);
            Mockito.doReturn(docTypeEntity).when(docTypesClientCall).getdocTypes(Mockito.any());

            DocumentResponse docResp = new DocumentResponse();
            Document document = new Document();
            document.setDocumentKey("keyFile");
            DocumentType documentTypeDoc = new DocumentType();
            documentTypeDoc.setChecksum(DocumentType.ChecksumEnum.MD5);
            document.setDocumentType(documentTypeDoc);
            docResp.setDocument(document);
            Mono<DocumentResponse> respDoc = Mono.just(docResp);
            Mockito.doReturn(respDoc).when(documentClientCall).postDocument(Mockito.any());

            WebTestClient.ResponseSpec responseSpec = fileUploadTestCall(fcr);
            FluxExchangeResult<FileCreationResponse> objectFluxExchangeResult =
                    responseSpec.expectStatus().isOk().returnResult(FileCreationResponse.class);
            FileCreationResponse resp = objectFluxExchangeResult.getResponseBody().blockFirst();

            Assertions.assertFalse(resp.getUploadUrl().isEmpty());
        }

        @Test
        void testIdClienteNoPermessiUpload() {
            FileCreationRequest fcr = new FileCreationRequest();
            fcr.setContentType(IMAGE_TIFF_VALUE);
            fcr.setDocumentType(PN_AAR);
            fcr.setStatus("");
            UserConfigurationResponse userConfig = new UserConfigurationResponse();
            UserConfiguration userConfiguration = new UserConfiguration();
            userConfiguration.setName(xPagoPaSafestorageCxIdValue);
            userConfiguration.setApiKey(xApiKeyValue);
            userConfiguration.setCanCreate(new ArrayList<>());
            userConfig.setUserConfiguration(userConfiguration);

            when(userConfigurationClientCall.getUser(anyString())).thenReturn(Mono.just(userConfig));
            when(documentClientCall.postDocument(any(DocumentInput.class))).thenReturn(Mono.just(DOCUMENT_RESPONSE));

            fileUploadTestCall(fcr).expectStatus().isForbidden();
        }

        @Test
        void testUrlGenerato() {

            log.debug("UriBulderUploadTest.testUrlGenerato() : decommentare");

            FileCreationRequest fcr = new FileCreationRequest();
            fcr.setContentType(IMAGE_TIFF_VALUE);
            fcr.setDocumentType(PN_AAR);
            fcr.setStatus("");
            FileCreationResponse fcresp = new FileCreationResponse();
            fcresp.setUploadUrl("http://host:9090/urlFile");

            UserConfigurationResponse userConfig = new UserConfigurationResponse();

            UserConfiguration userConfiguration = new UserConfiguration();
            userConfiguration.setName(xPagoPaSafestorageCxIdValue);
            userConfiguration.setApiKey(xApiKeyValue);
            userConfiguration.setCanCreate(List.of(PN_AAR));
            userConfig.setUserConfiguration(userConfiguration);

            Mono<UserConfigurationResponse> userConfigurationEntity = Mono.just(userConfig);
            Mockito.doReturn(userConfigurationEntity).when(userConfigurationClientCall).getUser(Mockito.any());


            DocumentTypeResponse documentTypeResponse = new DocumentTypeResponse();
            DocumentType documentType = new DocumentType();
            documentType.setTipoDocumento(PN_NOTIFICATION_ATTACHMENTS);
            documentTypeResponse.setDocType(documentType);

            Mono<DocumentTypeResponse> docTypeEntity = Mono.just(documentTypeResponse);
            Mockito.doReturn(docTypeEntity).when(docTypesClientCall).getdocTypes(Mockito.any());

            DocumentResponse docResp = new DocumentResponse();
            Document document = new Document();
            document.setDocumentKey("keyFile");
            DocumentType documentTypeDoc = new DocumentType();
            documentTypeDoc.setChecksum(DocumentType.ChecksumEnum.MD5);
            document.setDocumentType(documentTypeDoc);
            docResp.setDocument(document);
            Mono<DocumentResponse> respDoc = Mono.just(docResp);
            Mockito.doReturn(respDoc).when(documentClientCall).postDocument(Mockito.any());

            WebTestClient.ResponseSpec responseSpec = fileUploadTestCall(fcr);
            FluxExchangeResult<FileCreationResponse> objectFluxExchangeResult =
                    responseSpec.expectStatus().isOk().returnResult(FileCreationResponse.class);


            FileCreationResponse resp = objectFluxExchangeResult.getResponseBody().blockFirst();
            Assertions.assertFalse(resp.getUploadUrl().isEmpty());
        }

        @Test
        void testErroreInserimentoDocumentType() {

            UserConfigurationResponse userConfig = new UserConfigurationResponse();
            UserConfiguration userConfiguration = new UserConfiguration();
            userConfiguration.setName(xPagoPaSafestorageCxIdValue);
            userConfiguration.setApiKey(xApiKeyValue);
            userConfig.setUserConfiguration(userConfiguration);

            when(documentClientCall.postDocument(any(DocumentInput.class))).thenReturn(Mono.just(DOCUMENT_RESPONSE));
            when(userConfigurationClientCall.getUser(anyString())).thenReturn(Mono.just(userConfig));

            FileCreationRequest fcr = new FileCreationRequest();
            fcr.setContentType(IMAGE_TIFF_VALUE);
            fcr.setDocumentType("VALUE_FAULT");
            fcr.setStatus(PRELOADED);
            fileUploadTestCall(fcr).expectStatus().isBadRequest();
        }
    }
}
