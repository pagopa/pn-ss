package it.pagopa.pnss.uribuilder;

import static it.pagopa.pnss.common.Constant.IMAGE_TIFF;
import static it.pagopa.pnss.common.Constant.PN_AAR;
import static it.pagopa.pnss.common.Constant.PN_LEGAL_FACTS;
import static it.pagopa.pnss.common.Constant.PN_NOTIFICATION_ATTACHMENTS;
import static it.pagopa.pnss.common.Constant.PRELOADED;
import static it.pagopa.pnss.common.QueueNameConstant.BUCKET_HOT_NAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import it.pagopa.pn.template.internal.rest.v1.dto.*;
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

import it.pagopa.pn.template.rest.v1.dto.FileCreationRequest;
import it.pagopa.pn.template.rest.v1.dto.FileCreationResponse;
import it.pagopa.pnss.common.client.DocTypesClientCall;
import it.pagopa.pnss.common.client.DocumentClientCall;
import it.pagopa.pnss.common.client.UserConfigurationClientCall;
import it.pagopa.pnss.common.client.exception.DocumentKeyNotPresentException;
import it.pagopa.pnss.testutils.annotation.SpringBootTestWebEnv;
import it.pagopa.pnss.uribuilder.service.UriBuilderService;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;


@SpringBootTestWebEnv
@AutoConfigureWebTestClient
@Slf4j
public class UriBulderUploadTest {

    @Value("${header.x-api-key:#{null}}")
    private String xApiKey;
    @Value("${header.x-pagopa-safestorage-cx-id:#{null}}")
    private String X_PAGOPA_SAFESTORAGE_CX_ID;
//    public static final String X_PAGOPA_SAFESTORAGE_CX_ID = "x-pagopa-safestorage-cx-id";

    private static final String xApiKeyValue = "apiKey_value";
    private static final String xPagoPaSafestorageCxIdValue = "CLIENT_ID_123";
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
    DocumentClientCall documentClientCall;


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
                             .exchange();
    }

    private String getUploadFileEndpoint(String requestIdx) {
        return urlPath;
    }


    @Test
    void testUrlGenStatusPre() throws Exception {
    	
    	log.debug("UriBulderUploadTest.testUrlGenStatusPre() : decommentare");
    	
//        FileCreationRequest fcr = new FileCreationRequest();
//        fcr.setContentType(IMAGE_TIFF);
//        fcr.setDocumentType(PN_NOTIFICATION_ATTACHMENTS);
//        fcr.setStatus(PRELOADED);
//        FileCreationResponse fcresp = new FileCreationResponse();
//        fcresp.setUploadUrl("http://host:9090/urlFile");
//
//        UserConfigurationResponse userConfig = new UserConfigurationResponse();
//        UserConfiguration userConfiguration = new UserConfiguration();
//        userConfiguration.setName(xPagoPaSafestorageCxIdValue);
//        userConfiguration.setApiKey(xApiKeyValue);
//        userConfiguration.setCanCreate(List.of(PN_NOTIFICATION_ATTACHMENTS));
//        userConfig.setUserConfiguration(userConfiguration);
//
//        Mono<UserConfigurationResponse> userConfigurationEntity = Mono.just(userConfig);
//        Mockito.doReturn(userConfigurationEntity).when(userConfigurationClientCall).getUser(Mockito.any());
//
//        DocumentTypeResponse documentTypeResponse = new DocumentTypeResponse();
//        DocumentType documentType = new DocumentType();
//        documentType.setTipoDocumento(PN_LEGAL_FACTS);
//        documentTypeResponse.setDocType(documentType);
//
//        Mono<DocumentTypeResponse> docTypeEntity = Mono.just(documentTypeResponse);
//        Mockito.doReturn(docTypeEntity).when(docTypesClientCall).getdocTypes(Mockito.any());
//
//
//        Mockito.when(documentClientCall.getdocument(Mockito.any())).thenReturn(Mono.error(new DocumentKeyNotPresentException("keyFile")));
//
//        DocumentResponse docResp = new DocumentResponse();
//        Document document = new Document();
//        document.setDocumentKey("keyFile");
//        docResp.setDocument(document);
//        Mono<DocumentResponse> respDoc = Mono.just(docResp);
//        Mockito.doReturn(respDoc).when(documentClientCall).postDocument(Mockito.any());
//
//        WebTestClient.ResponseSpec responseSpec = fileUploadTestCall(BodyInserters.fromValue(fcr), X_PAGOPA_SAFESTORAGE_CX_ID);
//        FluxExchangeResult<FileCreationResponse> objectFluxExchangeResult =
//                responseSpec.expectStatus().isOk().returnResult(FileCreationResponse.class);
//        FileCreationResponse resp = objectFluxExchangeResult.getResponseBody().blockFirst();
//        S3Presigner presigner = uriBuilderService.getS3Presigner();
//
//
//        GetObjectRequest getObjectRequest = GetObjectRequest.builder().bucket(BUCKET_HOT_NAME).key(resp.getKey()).build();
//
//        GetObjectPresignRequest getObjectPresignRequest =
//                GetObjectPresignRequest.builder().signatureDuration(Duration.ofMinutes(60)).getObjectRequest(getObjectRequest).build();
//
//        PresignedGetObjectRequest presignedGetObjectRequest = presigner.presignGetObject(getObjectPresignRequest);
//        assertThat(presignedGetObjectRequest.url().toString()).startsWith(
//                "https://s3.eu-central-1.amazonaws.com/PnSsBucketName/" + resp.getKey());
//        assertThat(presignedGetObjectRequest.isBrowserExecutable()).isTrue();
//        assertThat(presignedGetObjectRequest.signedHeaders().get("host")).containsExactly("s3.eu-central-1.amazonaws.com");
//        assertThat(presignedGetObjectRequest.signedPayload()).isEmpty();


    }

    @Test
    void testStatoNonConsentito_PN_NOTIFICATION_ATTACHMENTS() {

        UserConfigurationResponse userConfig = new UserConfigurationResponse();
        UserConfiguration userConfiguration = new UserConfiguration();
        userConfiguration.setName(xPagoPaSafestorageCxIdValue);
        userConfiguration.setApiKey(xApiKeyValue);
        userConfig.setUserConfiguration(userConfiguration);

        when(userConfigurationClientCall.getUser(anyString())).thenReturn(Mono.just(userConfig));
        when(documentClientCall.postDocument(any(DocumentInput.class))).thenReturn(Mono.just(DOCUMENT_RESPONSE));


        FileCreationRequest fcr = new FileCreationRequest();
        fcr.setContentType(IMAGE_TIFF);
        fcr.setDocumentType(PN_NOTIFICATION_ATTACHMENTS);
        fcr.setStatus("ATTACHED");
        fileUploadTestCall(BodyInserters.fromValue(fcr), X_PAGOPA_SAFESTORAGE_CX_ID).expectStatus().isBadRequest();
    }

    @Test
    void testUrlGenerato() throws InterruptedException {
    	
    	log.debug("UriBulderUploadTest.testUrlGenerato() : decommentare");
    	
//        FileCreationRequest fcr = new FileCreationRequest();
//        fcr.setContentType(IMAGE_TIFF);
//        fcr.setDocumentType(PN_AAR);
//        fcr.setStatus("");
//        FileCreationResponse fcresp = new FileCreationResponse();
//        fcresp.setUploadUrl("http://host:9090/urlFile");
//
//        UserConfigurationResponse userConfig = new UserConfigurationResponse();
//
//        UserConfiguration userConfiguration = new UserConfiguration();
//        userConfiguration.setName(xPagoPaSafestorageCxIdValue);
//        userConfiguration.setApiKey(xApiKeyValue);
//        userConfiguration.setCanCreate(List.of(PN_AAR));
//        userConfig.setUserConfiguration(userConfiguration);
//
//        Mono<UserConfigurationResponse> userConfigurationEntity = Mono.just(userConfig);
//        Mockito.doReturn(userConfigurationEntity).when(userConfigurationClientCall).getUser(Mockito.any());
//
//        Mockito.when(documentClientCall.getdocument(Mockito.any())).thenReturn(Mono.error(new DocumentKeyNotPresentException("keyFile")));
//
//        DocumentTypeResponse documentTypeResponse = new DocumentTypeResponse();
//        DocumentType documentType = new DocumentType();
//        documentType.setTipoDocumento(PN_NOTIFICATION_ATTACHMENTS);
//        documentTypeResponse.setDocType(documentType);
//
//        Mono<DocumentTypeResponse> docTypeEntity = Mono.just(documentTypeResponse);
//        Mockito.doReturn(docTypeEntity).when(docTypesClientCall).getdocTypes(Mockito.any());
//
//        DocumentResponse docResp = new DocumentResponse();
//        Document document = new Document();
//        document.setDocumentKey("keyFile");
//        docResp.setDocument(document);
//        Mono<DocumentResponse> respDoc = Mono.just(docResp);
//        Mockito.doReturn(respDoc).when(documentClientCall).postDocument(Mockito.any());
//
//        WebTestClient.ResponseSpec responseSpec = fileUploadTestCall(BodyInserters.fromValue(fcr), X_PAGOPA_SAFESTORAGE_CX_ID);
//        FluxExchangeResult<FileCreationResponse> objectFluxExchangeResult =
//                responseSpec.expectStatus().isOk().returnResult(FileCreationResponse.class);
//        FileCreationResponse resp = objectFluxExchangeResult.getResponseBody().blockFirst();
//        S3Presigner presigner = uriBuilderService.getS3Presigner();
//
//
//        GetObjectRequest getObjectRequest = GetObjectRequest.builder().bucket(BUCKET_HOT_NAME).key(resp.getKey()).build();
//
//        GetObjectPresignRequest getObjectPresignRequest =
//                GetObjectPresignRequest.builder().signatureDuration(Duration.ofMinutes(60)).getObjectRequest(getObjectRequest).build();
//
//        PresignedGetObjectRequest presignedGetObjectRequest = presigner.presignGetObject(getObjectPresignRequest);
//        assertThat(presignedGetObjectRequest.url().toString()).startsWith(
//                "https://s3.eu-central-1.amazonaws.com/PnSsBucketName/" + resp.getKey());
//        assertThat(presignedGetObjectRequest.isBrowserExecutable()).isTrue();
//        assertThat(presignedGetObjectRequest.signedHeaders().get("host")).containsExactly("s3.eu-central-1.amazonaws.com");
//        assertThat(presignedGetObjectRequest.signedPayload()).isEmpty();

    }

    @Test
    void testStatoNonConsentito_PN_AAR() {

        UserConfigurationResponse userConfig = new UserConfigurationResponse();
        UserConfiguration userConfiguration = new UserConfiguration();
        userConfiguration.setName(xPagoPaSafestorageCxIdValue);
        userConfiguration.setApiKey(xApiKeyValue);
        userConfig.setUserConfiguration(userConfiguration);

        when(userConfigurationClientCall.getUser(anyString())).thenReturn(Mono.just(userConfig));
        when(documentClientCall.postDocument(any(DocumentInput.class))).thenReturn(Mono.just(DOCUMENT_RESPONSE));

        FileCreationRequest fcr = new FileCreationRequest();
        fcr.setContentType(IMAGE_TIFF);
        fcr.setDocumentType(PN_AAR);
        fcr.setStatus(PRELOADED);
        fileUploadTestCall(BodyInserters.fromValue(fcr), X_PAGOPA_SAFESTORAGE_CX_ID).expectStatus().isBadRequest();
    }


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
        fileUploadTestCall(BodyInserters.fromValue(fcr), X_PAGOPA_SAFESTORAGE_CX_ID).expectStatus().isBadRequest();
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
        fcr.setContentType(IMAGE_TIFF);
        fcr.setDocumentType("VALUE_FAULT");
        fcr.setStatus(PRELOADED);
        fileUploadTestCall(BodyInserters.fromValue(fcr), X_PAGOPA_SAFESTORAGE_CX_ID).expectStatus().isBadRequest();
    }

    @Test
    void testErroreInserimentoStatus() {

        UserConfigurationResponse userConfig = new UserConfigurationResponse();
        UserConfiguration userConfiguration = new UserConfiguration();
        userConfiguration.setName(xPagoPaSafestorageCxIdValue);
        userConfiguration.setApiKey(xApiKeyValue);
        userConfig.setUserConfiguration(userConfiguration);

        when(documentClientCall.postDocument(any(DocumentInput.class))).thenReturn(Mono.just(DOCUMENT_RESPONSE));
        when(userConfigurationClientCall.getUser(anyString())).thenReturn(Mono.just(userConfig));

        FileCreationRequest fcr = new FileCreationRequest();
        fcr.setContentType(IMAGE_TIFF);
        fcr.setDocumentType(PN_AAR);
        fcr.setStatus("VALUE_FAULT");

        fileUploadTestCall(BodyInserters.fromValue(fcr), X_PAGOPA_SAFESTORAGE_CX_ID).expectStatus().isBadRequest();
    }

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
        fcr.setDocumentType(PN_AAR);
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
        fcr.setDocumentType(PN_AAR);
        fcr.setStatus("VALUE_FAULT");

        fileUploadTestCall(BodyInserters.fromValue(fcr), X_PAGOPA_SAFESTORAGE_CX_ID).expectStatus().isBadRequest();
    }

    @Test
    void testIdClienteNonTrovatoUpload() {

        FileCreationRequest fcr = new FileCreationRequest();
        fcr.setContentType(IMAGE_TIFF);
        fcr.setDocumentType(PN_AAR);
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
        fcr.setContentType(IMAGE_TIFF);
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

        fileUploadTestCall(BodyInserters.fromValue(fcr), X_PAGOPA_SAFESTORAGE_CX_ID).expectStatus().isForbidden();
    }

}
