package it.pagopa.pnss.uribuilder;

import it.pagopa.pn.template.internal.rest.v1.dto.*;
import it.pagopa.pn.template.rest.v1.dto.FileCreationRequest;
import it.pagopa.pn.template.rest.v1.dto.FileCreationResponse;
import it.pagopa.pnss.common.ConstantTest;
import it.pagopa.pnss.common.client.DocTypesClientCall;
import it.pagopa.pnss.common.client.DocumentClientCall;
import it.pagopa.pnss.common.client.UserConfigurationClientCall;
import it.pagopa.pnss.configurationproperties.BucketName;
import it.pagopa.pnss.repositorymanager.service.DocTypesService;
import it.pagopa.pnss.testutils.annotation.SpringBootTestWebEnv;
import it.pagopa.pnss.uribuilder.service.UriBuilderService;
import lombok.extern.slf4j.Slf4j;

import org.assertj.core.util.Arrays;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ReactiveHttpOutputMessage;
import org.springframework.test.web.reactive.server.FluxExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserter;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static it.pagopa.pnss.common.constant.Constant.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;


@SpringBootTestWebEnv
@AutoConfigureWebTestClient
@Slf4j
class UriBuilderUploadTest {

    @Value("${header.x-api-key:#{null}}")
    private String xApiKey;
    @Value("${header.x-pagopa-safestorage-cx-id:#{null}}")
    private String X_PAGOPA_SAFESTORAGE_CX_ID;
    //    public static final String X_PAGOPA_SAFESTORAGE_CX_ID = "x-pagopa-safestorage-cx-id";
    @Value("${header.x-checksum-value:#{null}}")
    private String headerChecksumValue;

    private static final String xApiKeyValue = "apiKey_value";
    private static final String xPagoPaSafestorageCxIdValue = "CLIENT_ID_123";
    private static final String xChecksumValue = "checkSumValue";
    private static final DocumentResponse DOCUMENT_RESPONSE = new DocumentResponse().document(new Document().documentKey("documentKey"));

    @Value("${file.upload.api.url}")
    private String urlPath;

    @Autowired
    private WebTestClient webClient;

    @MockBean
    DocTypesClientCall docTypesClientCall;
    @MockBean
    UserConfigurationClientCall userConfigurationClientCall;
    @Autowired
    private UriBuilderService uriBuilderService;
    @MockBean
    DocTypesService docTypesService;

    @MockBean
    DocumentClientCall documentClientCall;

    @Autowired
    BucketName bucketName;

    private WebTestClient.ResponseSpec fileUploadTestCall(BodyInserter<FileCreationRequest, ReactiveHttpOutputMessage> bodyInserter,
                                                          String requestIdx) {
        this.webClient.mutate().responseTimeout(Duration.ofMillis(30000)).build();

        return this.webClient.post()
                             .uri(getUploadFileEndpoint(requestIdx))
                             .accept(MediaType.APPLICATION_JSON)
                             .contentType(MediaType.APPLICATION_JSON)
                             .body(bodyInserter)
                             .header(X_PAGOPA_SAFESTORAGE_CX_ID, xPagoPaSafestorageCxIdValue)
                             .header(xApiKey, xApiKeyValue)
                             .header(headerChecksumValue, xChecksumValue)
                             .exchange();
    }

    private String getUploadFileEndpoint(String requestIdx) {
        return urlPath;
    }


    @Test
    void testUrlGenStatusPre() throws Exception {

        log.debug("UriBulderUploadTest.testUrlGenStatusPre() : decommentare");

        FileCreationRequest fcr = new FileCreationRequest();
        fcr.setContentType(IMAGE_TIFF_VALUE);
        fcr.setDocumentType(ConstantTest.PN_NOTIFICATION_ATTACHMENTS);
        fcr.setStatus(PRELOADED);
        FileCreationResponse fcresp = new FileCreationResponse();
        fcresp.setUploadUrl("http://host:9090/urlFile");

        UserConfigurationResponse userConfig = new UserConfigurationResponse();
        UserConfiguration userConfiguration = new UserConfiguration();
        userConfiguration.setName(xPagoPaSafestorageCxIdValue);
        userConfiguration.setApiKey(xApiKeyValue);
        userConfiguration.setCanCreate(List.of(ConstantTest.PN_NOTIFICATION_ATTACHMENTS));
        userConfig.setUserConfiguration(userConfiguration);

        Mono<UserConfigurationResponse> userConfigurationEntity = Mono.just(userConfig);
        Mockito.doReturn(userConfigurationEntity).when(userConfigurationClientCall).getUser(Mockito.any());

        DocumentTypeResponse documentTypeResponse = new DocumentTypeResponse();
        DocumentType documentType = new DocumentType();
        documentType.setTipoDocumento(ConstantTest.PN_LEGAL_FACTS);
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

        WebTestClient.ResponseSpec responseSpec = fileUploadTestCall(BodyInserters.fromValue(fcr), X_PAGOPA_SAFESTORAGE_CX_ID);
        FluxExchangeResult<FileCreationResponse> objectFluxExchangeResult =
                responseSpec.expectStatus().isOk().returnResult(FileCreationResponse.class);
        FileCreationResponse resp = objectFluxExchangeResult.getResponseBody().blockFirst();

        Assertions.assertFalse(resp.getUploadUrl().isEmpty());
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

    @Test
    void testUrlGenerato() throws InterruptedException {

        log.debug("UriBulderUploadTest.testUrlGenerato() : decommentare");

        FileCreationRequest fcr = new FileCreationRequest();
        fcr.setContentType(IMAGE_TIFF_VALUE);
        fcr.setDocumentType(ConstantTest.PN_AAR);
        fcr.setStatus("");
        FileCreationResponse fcresp = new FileCreationResponse();
        fcresp.setUploadUrl("http://host:9090/urlFile");

        UserConfigurationResponse userConfig = new UserConfigurationResponse();

        UserConfiguration userConfiguration = new UserConfiguration();
        userConfiguration.setName(xPagoPaSafestorageCxIdValue);
        userConfiguration.setApiKey(xApiKeyValue);
        userConfiguration.setCanCreate(List.of(ConstantTest.PN_AAR));
        userConfig.setUserConfiguration(userConfiguration);

        Mono<UserConfigurationResponse> userConfigurationEntity = Mono.just(userConfig);
        Mockito.doReturn(userConfigurationEntity).when(userConfigurationClientCall).getUser(Mockito.any());


        DocumentTypeResponse documentTypeResponse = new DocumentTypeResponse();
        DocumentType documentType = new DocumentType();
        documentType.setTipoDocumento(ConstantTest.PN_NOTIFICATION_ATTACHMENTS);
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

        WebTestClient.ResponseSpec responseSpec = fileUploadTestCall(BodyInserters.fromValue(fcr), X_PAGOPA_SAFESTORAGE_CX_ID);
        FluxExchangeResult<FileCreationResponse> objectFluxExchangeResult =
                responseSpec.expectStatus().isOk().returnResult(FileCreationResponse.class);


        FileCreationResponse resp = objectFluxExchangeResult.getResponseBody().blockFirst();
        Assertions.assertFalse(resp.getUploadUrl().isEmpty());
    }

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


//    @Test
//    void testErroreInserimentoContentType() {
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
//        fcr.setContentType("VALUE_FAULT");
//        fcr.setDocumentType(ConstantTest.PN_AAR);
//        fcr.setStatus(PRELOADED);
//        fileUploadTestCall(BodyInserters.fromValue(fcr), X_PAGOPA_SAFESTORAGE_CX_ID).expectStatus().isBadRequest();
//    }

//    @Test
//    void testErroreInserimentoDocumentType() {
//
//        UserConfigurationResponse userConfig = new UserConfigurationResponse();
//        UserConfiguration userConfiguration = new UserConfiguration();
//        userConfiguration.setName(xPagoPaSafestorageCxIdValue);
//        userConfiguration.setApiKey(xApiKeyValue);
//        userConfig.setUserConfiguration(userConfiguration);
//        
//        DocumentType documentType = new DocumentType();
//        documentType.setTipoDocumento(ConstantTest.PN_NOTIFICATION_ATTACHMENTS);
//        List<DocumentType> documentTypeList = new ArrayList<>();
//        documentTypeList.add(documentType);
//
//        when(documentClientCall.postDocument(any(DocumentInput.class))).thenReturn(Mono.just(DOCUMENT_RESPONSE));
//        when(userConfigurationClientCall.getUser(anyString())).thenReturn(Mono.just(userConfig));
//        //when(docTypesService.getAllDocumentType()).thenReturn(Mono.just(documentTypeList));
//
//        FileCreationRequest fcr = new FileCreationRequest();
//        fcr.setContentType(IMAGE_TIFF_VALUE);
//        fcr.setDocumentType("VALUE_FAULT");
//        fcr.setStatus(PRELOADED);
//        fileUploadTestCall(BodyInserters.fromValue(fcr), X_PAGOPA_SAFESTORAGE_CX_ID).expectStatus().isBadRequest();
//    }

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
    void testContetTypeParamObbligatorio() {

        UserConfigurationResponse userConfig = new UserConfigurationResponse();
        UserConfiguration userConfiguration = new UserConfiguration();
        userConfiguration.setName(xPagoPaSafestorageCxIdValue);
        userConfiguration.setApiKey(xApiKeyValue);
        userConfig.setUserConfiguration(userConfiguration);

        when(documentClientCall.postDocument(any(DocumentInput.class))).thenReturn(Mono.just(DOCUMENT_RESPONSE));
        when(userConfigurationClientCall.getUser(anyString())).thenReturn(Mono.just(userConfig));

        FileCreationRequest fcr = new FileCreationRequest();
        fcr.setDocumentType(ConstantTest.PN_AAR);
        fcr.setStatus("VALUE_FAULT");

        fileUploadTestCall(BodyInserters.fromValue(fcr), X_PAGOPA_SAFESTORAGE_CX_ID).expectStatus().isBadRequest();
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
        fcr.setDocumentType(ConstantTest.PN_AAR);
        fcr.setStatus("VALUE_FAULT");

        fileUploadTestCall(BodyInserters.fromValue(fcr), X_PAGOPA_SAFESTORAGE_CX_ID).expectStatus().isBadRequest();
    }

    @Test
    void testIdClienteNonTrovatoUpload() {

        FileCreationRequest fcr = new FileCreationRequest();
        fcr.setContentType(IMAGE_TIFF_VALUE);
        fcr.setDocumentType(ConstantTest.PN_AAR);
        fcr.setStatus("");

        Mono<UserConfigurationResponse> userConfigurationEntity = Mono.empty();

        Mockito.doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "User Not Found : "))
               .when(userConfigurationClientCall)
               .getUser(Mockito.any());

        fileUploadTestCall(BodyInserters.fromValue(fcr), X_PAGOPA_SAFESTORAGE_CX_ID).expectStatus().isNotFound();
    }

    @Test
    void testIdClienteNoPermessiUpload() {
        FileCreationRequest fcr = new FileCreationRequest();
        fcr.setContentType(IMAGE_TIFF_VALUE);
        fcr.setDocumentType(ConstantTest.PN_AAR);
        fcr.setStatus("");
        UserConfigurationResponse userConfig = new UserConfigurationResponse();
        UserConfiguration userConfiguration = new UserConfiguration();
        userConfiguration.setName(xPagoPaSafestorageCxIdValue);
        userConfiguration.setApiKey(xApiKeyValue);
        userConfiguration.setCanCreate(new ArrayList<>());
        userConfig.setUserConfiguration(userConfiguration);

        when(userConfigurationClientCall.getUser(anyString())).thenReturn(Mono.just(userConfig));
        when(documentClientCall.postDocument(any(DocumentInput.class))).thenReturn(Mono.just(DOCUMENT_RESPONSE));

        fileUploadTestCall(BodyInserters.fromValue(fcr), X_PAGOPA_SAFESTORAGE_CX_ID).expectStatus().isForbidden();
    }

}
