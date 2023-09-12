package it.pagopa.pnss.uribuilder;

import it.pagopa.pn.template.internal.rest.v1.dto.*;
import it.pagopa.pn.template.internal.rest.v1.dto.DocumentType.ChecksumEnum;
import it.pagopa.pn.template.internal.rest.v1.dto.DocumentType.TransformationsEnum;
import it.pagopa.pn.template.rest.v1.dto.FileCreationRequest;
import it.pagopa.pn.template.rest.v1.dto.FileCreationResponse;
import it.pagopa.pnss.common.client.DocTypesClientCall;
import it.pagopa.pnss.common.client.DocumentClientCall;
import it.pagopa.pnss.common.client.UserConfigurationClientCall;
import it.pagopa.pnss.configurationproperties.RepositoryManagerDynamoTableName;
import it.pagopa.pnss.repositorymanager.entity.DocTypeEntity;
import it.pagopa.pnss.repositorymanager.service.DocTypesService;
import it.pagopa.pnss.testutils.annotation.SpringBootTestWebEnv;
import lombok.extern.slf4j.Slf4j;

import org.eclipse.jetty.http.HttpHeader;
import org.junit.jupiter.api.*;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.FluxExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static it.pagopa.pnss.common.DocTypesConstant.*;
import static it.pagopa.pnss.common.constant.Constant.*;
import static org.mockito.ArgumentMatchers.*;
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
    private DocumentClientCall documentClientCall;

    @Value("${header.x-api-key:#{null}}")
    private String xApiKey;

    @Value("${header.x-pagopa-safestorage-cx-id:#{null}}")
    private String X_PAGOPA_SAFESTORAGE_CX_ID;

    @Value("${queryParam.presignedUrl.traceId:#{null}}")
    private String queryParamPresignedUrlTraceId;

    @Value("${header.x-checksum-value:#{null}}")
    private String headerChecksumValue;

    @Value("${file.upload.api.url}")
    private String urlPath;

    private static final String xApiKeyValue = "apiKey_value";
    private static final String xPagoPaSafestorageCxIdValue = "CLIENT_ID_123";
    private static final String xChecksumValue = "checkSumValue";
    private static final String X_QUERY_PARAM_URL_VALUE= "queryParamPresignedUrlTraceId_value";
    private static final DocumentResponse DOCUMENT_RESPONSE = new DocumentResponse().document(new Document().documentKey("documentKey").documentType(new DocumentType().checksum(ChecksumEnum.MD5)));

    private static DynamoDbTable<DocTypeEntity> dynamoDbTable;

    private WebTestClient.RequestHeadersSpec callRequestHeadersSpec(FileCreationRequest fileCreationRequest)
    {
        return this.webClient.post()
                .uri(urlPath)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(fileCreationRequest)
                .header(X_PAGOPA_SAFESTORAGE_CX_ID, xPagoPaSafestorageCxIdValue)
                .header(xApiKey, xApiKeyValue);
    }
    private WebTestClient.ResponseSpec fileUploadTestCall(FileCreationRequest fileCreationRequest) {

        return callRequestHeadersSpec(fileCreationRequest)
                .header(queryParamPresignedUrlTraceId, X_QUERY_PARAM_URL_VALUE)
                .header(headerChecksumValue, xChecksumValue)
                .exchange();
    }

    private WebTestClient.ResponseSpec fileUploadTestCallNoHeader(FileCreationRequest fileCreationRequest) {

        return callRequestHeadersSpec(fileCreationRequest)
                .header(queryParamPresignedUrlTraceId, X_QUERY_PARAM_URL_VALUE)
                .exchange();
    }

    private WebTestClient.ResponseSpec noTraceIdfileUploadTestCall(FileCreationRequest fileCreationRequest) {

        return callRequestHeadersSpec(fileCreationRequest)
                .header(headerChecksumValue, xChecksumValue)
                .exchange();
    }

    private WebTestClient.ResponseSpec noChecksumUploadTestCall(FileCreationRequest fileCreationRequest) {

        return callRequestHeadersSpec(fileCreationRequest)
                .header(queryParamPresignedUrlTraceId, X_QUERY_PARAM_URL_VALUE)
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
    void testMissingTraceIdHeader()
    {
        UserConfigurationResponse userConfig = new UserConfigurationResponse();
        UserConfiguration userConfiguration = new UserConfiguration();
        userConfiguration.setName(xPagoPaSafestorageCxIdValue);
        userConfiguration.setApiKey(xApiKeyValue);
        userConfiguration.setCanCreate(List.of(PN_AAR));
        userConfig.setUserConfiguration(userConfiguration);

        when(documentClientCall.postDocument(any(DocumentInput.class))).thenReturn(Mono.just(DOCUMENT_RESPONSE));
        when(userConfigurationClientCall.getUser(anyString())).thenReturn(Mono.just(userConfig));
        when(docTypesClientCall.getdocTypes(anyString())).thenReturn(Mono.just(new DocumentTypeResponse().docType(new DocumentType().transformations(List.of(TransformationsEnum.SIGN_AND_TIMEMARK)))));

        FileCreationRequest fcr = new FileCreationRequest();
        fcr.setContentType("application/pdf");
        fcr.setDocumentType(PN_AAR);
        fcr.setStatus(PRELOADED);
        noTraceIdfileUploadTestCall(fcr).expectStatus().isBadRequest();
    }

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
        void testUrlGenStatusPreFileKeyPrefixed() {
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
            document.setDocumentKey("2023/09/01/11/keyFile");
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

            DocumentTypeResponse documentTypeResponse = new DocumentTypeResponse();
            DocumentType documentType = new DocumentType();
            documentType.setTipoDocumento(PN_NOTIFICATION_ATTACHMENTS);
            documentTypeResponse.setDocType(documentType);

            Mono<DocumentTypeResponse> docTypeEntity = Mono.just(documentTypeResponse);
            Mockito.doReturn(docTypeEntity).when(docTypesClientCall).getdocTypes(Mockito.any());

            when(userConfigurationClientCall.getUser(anyString())).thenReturn(Mono.just(userConfig));
            when(documentClientCall.postDocument(any(DocumentInput.class))).thenReturn(Mono.just(DOCUMENT_RESPONSE));

            fileUploadTestCall(fcr).expectStatus().isForbidden();
        }

        @Test
        void testUrlGeneratoMD5(){
            testUrlGenerato(ChecksumEnum.MD5);
        }

        @Test
        void testUrlGeneratoNONE(){
            testUrlGenerato(ChecksumEnum.NONE);
        }

        void testUrlGenerato(ChecksumEnum checksumValue) {

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
            documentTypeDoc.setChecksum((checksumValue));
            document.setDocumentType(documentTypeDoc);
            docResp.setDocument(document);
            Mono<DocumentResponse> respDoc = Mono.just(docResp);
            Mockito.doReturn(respDoc).when(documentClientCall).postDocument(Mockito.any());

            WebTestClient.ResponseSpec responseSpec;

            if(checksumValue.equals(ChecksumEnum.MD5)){
                responseSpec = fileUploadTestCall(fcr);
            }else{
                responseSpec = fileUploadTestCallNoHeader(fcr);
            }

            FluxExchangeResult<FileCreationResponse> objectFluxExchangeResult =
                    responseSpec.expectStatus().isOk().returnResult(FileCreationResponse.class);
            FileCreationResponse resp = objectFluxExchangeResult.getResponseBody().blockFirst();
            Assertions.assertFalse(resp.getUploadUrl().isEmpty());
        }
        @Test
        void testUploadSignedPdfContentTypeOk() {

            UserConfigurationResponse userConfig = new UserConfigurationResponse();
            UserConfiguration userConfiguration = new UserConfiguration();
            userConfiguration.setName(xPagoPaSafestorageCxIdValue);
            userConfiguration.setApiKey(xApiKeyValue);
            userConfiguration.setCanCreate(List.of(PN_AAR));
            userConfig.setUserConfiguration(userConfiguration);

            when(documentClientCall.postDocument(any(DocumentInput.class))).thenReturn(Mono.just(DOCUMENT_RESPONSE));
            when(userConfigurationClientCall.getUser(anyString())).thenReturn(Mono.just(userConfig));
            when(docTypesClientCall.getdocTypes(anyString())).thenReturn(Mono.just(new DocumentTypeResponse().docType(new DocumentType().transformations(List.of(TransformationsEnum.SIGN_AND_TIMEMARK)))));

            FileCreationRequest fcr = new FileCreationRequest();
            fcr.setContentType("application/pdf");
            fcr.setDocumentType(PN_AAR);
            fcr.setStatus(PRELOADED);
            fileUploadTestCall(fcr).expectStatus().isOk();
        }

        @Test
        void testUploadSignedPdfBadContentType() {

            UserConfigurationResponse userConfig = new UserConfigurationResponse();
            UserConfiguration userConfiguration = new UserConfiguration();
            userConfiguration.setName(xPagoPaSafestorageCxIdValue);
            userConfiguration.setApiKey(xApiKeyValue);
            userConfiguration.setCanCreate(List.of(PN_AAR));
            userConfig.setUserConfiguration(userConfiguration);

            when(documentClientCall.postDocument(any(DocumentInput.class))).thenReturn(Mono.just(DOCUMENT_RESPONSE));
            when(userConfigurationClientCall.getUser(anyString())).thenReturn(Mono.just(userConfig));
            when(docTypesClientCall.getdocTypes(anyString())).thenReturn(Mono.just(new DocumentTypeResponse().docType(new DocumentType().transformations(List.of(TransformationsEnum.SIGN_AND_TIMEMARK)))));

            FileCreationRequest fcr = new FileCreationRequest();
            fcr.setContentType("application/badContentType");
            fcr.setDocumentType(PN_AAR);
            fcr.setStatus(PRELOADED);
            fileUploadTestCall(fcr).expectStatus().isOk();
        }
    }
}
