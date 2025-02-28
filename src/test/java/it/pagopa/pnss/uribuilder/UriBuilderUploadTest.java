package it.pagopa.pnss.uribuilder;

import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.*;
import it.pagopa.pnss.common.client.DocTypesClientCall;
import it.pagopa.pnss.common.client.DocumentClientCall;
import it.pagopa.pnss.common.client.TagsClientCall;
import it.pagopa.pnss.common.client.UserConfigurationClientCall;
import it.pagopa.pnss.common.client.exception.DocumentKeyNotPresentException;
import it.pagopa.pnss.common.exception.PutTagsBadRequestException;
import it.pagopa.pnss.repositorymanager.entity.DocTypeEntity;
import it.pagopa.pnss.repositorymanager.service.DocTypesService;
import it.pagopa.pnss.testutils.annotation.SpringBootTestWebEnv;
import lombok.CustomLog;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
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
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;


import java.util.*;

import static it.pagopa.pnss.common.DocTypesConstant.*;
import static it.pagopa.pnss.common.constant.Constant.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;


@SpringBootTestWebEnv
@AutoConfigureWebTestClient(timeout = "36000")
@CustomLog
class UriBuilderUploadTest {

    @Autowired
    private WebTestClient webClient;

    @MockBean
    private DocTypesClientCall docTypesClientCall;

    @MockBean
    private UserConfigurationClientCall userConfigurationClientCall;

    @MockBean
    private DocumentClientCall documentClientCall;

    @MockBean
    private TagsClientCall tagsClientCall;

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
    private static final DocumentResponse DOCUMENT_RESPONSE = new DocumentResponse().document(new Document().documentKey("documentKey").documentType(new DocumentType().checksum(DocumentType.ChecksumEnum.MD5)));
    private static final DocumentResponse DOCUMENT_RESPONSE_TAGS = new DocumentResponse().document(new Document().documentKey("documentKey").tags(createTagsList()).documentType(new DocumentType().checksum(DocumentType.ChecksumEnum.MD5)));
    private static final String IUN = "IUN";

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
                .header("x-checksum-value", "checkumValueExample")
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

    private static Map<String, List<String>> createTagsList(){
        Map<String, List<String>> tags = new HashMap<>();

        List<String> valuesForKey1 = new ArrayList<>();
        valuesForKey1.add("value_1");
        valuesForKey1.add("value_7");
        tags.put("key_1", valuesForKey1);

        List<String> valuesForKey2 = new ArrayList<>();
        valuesForKey2.add("value_2");
        tags.put("key_2", valuesForKey2);

        List<String> valuesForKey3 = new ArrayList<>();
        valuesForKey3.add("value_3");
        valuesForKey3.add("value_9");
        valuesForKey3.add("value_8");
        tags.put("key_3", valuesForKey3);

        List<String> valuesForKey4 = new ArrayList<>();
        valuesForKey4.add("value_4");
        tags.put("key_4", valuesForKey4);
        return tags;
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
        when(docTypesClientCall.getdocTypes(anyString())).thenReturn(Mono.just(new DocumentTypeResponse().docType(new DocumentType().transformations(List.of("SIGN_AND_TIMEMARK")))));

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
            userConfiguration.setCanWriteTags(true);
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
            testUrlGenerato(DocumentType.ChecksumEnum.MD5);
        }

        @Test
        void testUrlGeneratoNONE(){
            testUrlGenerato(DocumentType.ChecksumEnum.NONE);
        }

        void testUrlGenerato(DocumentType.ChecksumEnum checksumValue) {

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
            userConfiguration.setCanWriteTags(true);
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

            if(checksumValue.equals(DocumentType.ChecksumEnum.MD5)){
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
            userConfiguration.setCanWriteTags(true);
            userConfig.setUserConfiguration(userConfiguration);

            when(documentClientCall.postDocument(any(DocumentInput.class))).thenReturn(Mono.just(DOCUMENT_RESPONSE));
            when(userConfigurationClientCall.getUser(anyString())).thenReturn(Mono.just(userConfig));
            when(docTypesClientCall.getdocTypes(anyString())).thenReturn(Mono.just(new DocumentTypeResponse().docType(new DocumentType().transformations(List.of("SIGN_AND_TIMEMARK")))));

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
            userConfiguration.setCanWriteTags(true);
            userConfig.setUserConfiguration(userConfiguration);

            when(documentClientCall.postDocument(any(DocumentInput.class))).thenReturn(Mono.just(DOCUMENT_RESPONSE));
            when(userConfigurationClientCall.getUser(anyString())).thenReturn(Mono.just(userConfig));
            when(docTypesClientCall.getdocTypes(anyString())).thenReturn(Mono.just(new DocumentTypeResponse().docType(new DocumentType().transformations(List.of("SIGN_AND_TIMEMARK")))));

            FileCreationRequest fcr = new FileCreationRequest();
            fcr.setContentType("application/badContentType");
            fcr.setDocumentType(PN_AAR);
            fcr.setStatus(PRELOADED);
            fileUploadTestCall(fcr).expectStatus().isOk();
        }
        @Test
        void testUploadSignedPdfContentTypeTagsOk() {

            UserConfigurationResponse userConfig = new UserConfigurationResponse();
            UserConfiguration userConfiguration = new UserConfiguration();
            userConfiguration.setName(xPagoPaSafestorageCxIdValue);
            userConfiguration.setApiKey(xApiKeyValue);
            userConfiguration.setCanCreate(List.of(PN_AAR));
            userConfiguration.setCanWriteTags(true);
            userConfig.setUserConfiguration(userConfiguration);

            String tagValue = "ABCDEF";
            Map<String, List<String>> setTags = Map.of(IUN, List.of(tagValue));

            when(documentClientCall.postDocument(any(DocumentInput.class))).thenReturn(Mono.just(DOCUMENT_RESPONSE_TAGS));
            when(userConfigurationClientCall.getUser(anyString())).thenReturn(Mono.just(userConfig));
            when(docTypesClientCall.getdocTypes(anyString())).thenReturn(Mono.just(new DocumentTypeResponse().docType(new DocumentType().transformations(List.of("SIGN_AND_TIMEMARK")))));
            when(tagsClientCall.putTags("documentKey",new TagsChanges().SET(setTags))).thenReturn(Mono.just(new TagsResponse()));

            FileCreationRequest fcr = new FileCreationRequest();
            fcr.setContentType("application/pdf");
            fcr.setDocumentType(PN_AAR);
            fcr.setStatus(PRELOADED);
            fileUploadTestCall(fcr).expectStatus().isOk();
            log.info("TAGS DOC {}", fcr);
        }
        @Test
        void createFileWithTags() {
            FileCreationRequest fcr = createFileCreationRequest();

            UserConfigurationResponse userConfig = new UserConfigurationResponse();
            UserConfiguration userConfiguration = new UserConfiguration();
            userConfiguration.setName(xPagoPaSafestorageCxIdValue);
            userConfiguration.setApiKey(xApiKeyValue);
            userConfiguration.setCanCreate(List.of(PN_AAR));
            userConfiguration.setCanWriteTags(true);
            userConfig.setUserConfiguration(userConfiguration);

            when(documentClientCall.postDocument(any(DocumentInput.class))).thenReturn(Mono.just(DOCUMENT_RESPONSE_TAGS));
            when(userConfigurationClientCall.getUser(anyString())).thenReturn(Mono.just(userConfig));
            when(docTypesClientCall.getdocTypes(anyString())).thenReturn(Mono.just(new DocumentTypeResponse().docType(new DocumentType().transformations(List.of("SIGN_AND_TIMEMARK")))));
            when(tagsClientCall.putTags("documentKey",new TagsChanges().SET(fcr.getTags()))).thenReturn(Mono.just(new TagsResponse()));

            fileUploadTestCall(fcr).expectStatus().isOk();


        }
        private FileCreationRequest createFileCreationRequest() {
            FileCreationRequest fcr = new FileCreationRequest();
            fcr.setContentType("application/pdf");
            fcr.setDocumentType(PN_AAR);
            fcr.setStatus(PRELOADED);
            Map<String, List<String>> tags = new HashMap<>();
            tags.put("CONSERVATION_STATUS", Collections.singletonList("OK"));
            tags.put(IUN, Arrays.asList("XXXFEF3RFD", "CHDGDTFENM"));
            fcr.setTags(tags);
            return fcr;
        }
        @Test
        void createFile_MaxTagsPerRequest_Ko() {
            FileCreationRequest fileCreationRequest = createFileCreationRequest();
            fileCreationRequest.setTags(generateRandomTags().block());

            UserConfigurationResponse userConfig = new UserConfigurationResponse();
            UserConfiguration userConfiguration = new UserConfiguration();
            userConfiguration.setName(xPagoPaSafestorageCxIdValue);
            userConfiguration.setApiKey(xApiKeyValue);
            userConfiguration.setCanCreate(List.of(PN_AAR));
            userConfiguration.setCanWriteTags(true);
            userConfig.setUserConfiguration(userConfiguration);

            when(userConfigurationClientCall.getUser(anyString())).thenReturn(Mono.just(userConfig));
            when(documentClientCall.postDocument(any(DocumentInput.class))).thenReturn(Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Exceeded MaxTagsPerRequest limit")));
            when(docTypesClientCall.getdocTypes(anyString())).thenReturn(Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Exceeded MaxTagsPerRequest limit")));

            fileUploadTestCall(fileCreationRequest)
                    .expectStatus().isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        void createFile_MaxValuesPerTagPerRequest_Ko() {
            FileCreationRequest fileCreationRequest = createFileCreationRequest();
            Map<String, List<String>> tags = generateRandomTagsWithMaxValuesExceeded();
            fileCreationRequest.setTags(tags);

            UserConfigurationResponse userConfig = new UserConfigurationResponse();
            UserConfiguration userConfiguration = new UserConfiguration();
            userConfiguration.setName(xPagoPaSafestorageCxIdValue);
            userConfiguration.setApiKey(xApiKeyValue);
            userConfiguration.setCanCreate(List.of(PN_AAR));
            userConfiguration.setCanWriteTags(true);
            userConfig.setUserConfiguration(userConfiguration);

            String exceedingKey = tags.entrySet().stream()
                    .filter(entry -> entry.getValue().size() > 50)
                    .map(Map.Entry::getKey)
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("Nessuna chiave con limite di valori superato"));

            when(userConfigurationClientCall.getUser(anyString())).thenReturn(Mono.just(userConfig));
            when(documentClientCall.postDocument(any(DocumentInput.class))).thenReturn(Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Exceeded MaxValuesPerTagPerRequest limit for tag: " +exceedingKey)));
            when(docTypesClientCall.getdocTypes(anyString())).thenReturn(Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Exceeded MaxValuesPerTagPerRequest limit for tag: " + exceedingKey)));

            fileUploadTestCall(fileCreationRequest)
                    .expectStatus().isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        void createFileWithTags_PutTagsBadRequest_Ko() {
            FileCreationRequest fcr = createFileCreationRequest();

            UserConfigurationResponse userConfig = new UserConfigurationResponse();
            UserConfiguration userConfiguration = new UserConfiguration();
            userConfiguration.setName(xPagoPaSafestorageCxIdValue);
            userConfiguration.setApiKey(xApiKeyValue);
            userConfiguration.setCanCreate(List.of(PN_AAR));
            userConfiguration.setCanWriteTags(true);
            userConfig.setUserConfiguration(userConfiguration);

            when(documentClientCall.postDocument(any(DocumentInput.class))).thenReturn(Mono.just(DOCUMENT_RESPONSE_TAGS));
            when(userConfigurationClientCall.getUser(anyString())).thenReturn(Mono.just(userConfig));
            when(docTypesClientCall.getdocTypes(anyString())).thenReturn(Mono.just(new DocumentTypeResponse().docType(new DocumentType().transformations(List.of("SIGN_AND_TIMEMARK")))));
            when(tagsClientCall.putTags("documentKey", new TagsChanges().SET(fcr.getTags()))).thenReturn(Mono.error(new PutTagsBadRequestException()));

            fileUploadTestCall(fcr).expectStatus().isBadRequest();
        }

        @ParameterizedTest
        @NullSource
        @ValueSource(booleans = {false})
        void createFileWithTags_PutTagsForbidden_Ko(Boolean canWriteTags) {
            FileCreationRequest fcr = createFileCreationRequest();

            UserConfigurationResponse userConfig = new UserConfigurationResponse();
            UserConfiguration userConfiguration = new UserConfiguration();
            userConfiguration.setName(xPagoPaSafestorageCxIdValue);
            userConfiguration.setApiKey(xApiKeyValue);
            userConfiguration.setCanCreate(List.of(PN_AAR));
            userConfiguration.setCanWriteTags(canWriteTags);
            userConfig.setUserConfiguration(userConfiguration);

            when(documentClientCall.postDocument(any(DocumentInput.class))).thenReturn(Mono.just(DOCUMENT_RESPONSE_TAGS));
            when(userConfigurationClientCall.getUser(anyString())).thenReturn(Mono.just(userConfig));
            when(docTypesClientCall.getdocTypes(anyString())).thenReturn(Mono.just(new DocumentTypeResponse().docType(new DocumentType().transformations(List.of("SIGN_AND_TIMEMARK")))));
            when(tagsClientCall.putTags("documentKey", new TagsChanges().SET(fcr.getTags()))).thenReturn(Mono.error(new PutTagsBadRequestException()));

            fileUploadTestCall(fcr).expectStatus().isForbidden();
        }

        @Test
        void createFileWithTags_DocumentKeyNotFound_Ko() {
            FileCreationRequest fcr = createFileCreationRequest();

            UserConfigurationResponse userConfig = new UserConfigurationResponse();
            UserConfiguration userConfiguration = new UserConfiguration();
            userConfiguration.setName(xPagoPaSafestorageCxIdValue);
            userConfiguration.setApiKey(xApiKeyValue);
            userConfiguration.setCanCreate(List.of(PN_AAR));
            userConfiguration.setCanWriteTags(true);
            userConfig.setUserConfiguration(userConfiguration);

            when(documentClientCall.postDocument(any(DocumentInput.class))).thenReturn(Mono.just(DOCUMENT_RESPONSE_TAGS));
            when(userConfigurationClientCall.getUser(anyString())).thenReturn(Mono.just(userConfig));
            when(docTypesClientCall.getdocTypes(anyString())).thenReturn(Mono.just(new DocumentTypeResponse().docType(new DocumentType().transformations(List.of("SIGN_AND_TIMEMARK")))));
            when(tagsClientCall.putTags("documentKey", new TagsChanges().SET(fcr.getTags()))).thenReturn(Mono.error(new DocumentKeyNotPresentException("documentKey")));

            fileUploadTestCall(fcr).expectStatus().isNotFound();
        }

        private Map<String, List<String>> generateRandomTagsWithMaxValuesExceeded() {
            Map<String, List<String>> tags = new HashMap<>();

            List<String> valuesExceedingLimit = new ArrayList<>();
            for (int i = 0; i < 51; i++) {
                valuesExceedingLimit.add(UUID.randomUUID().toString());
            }
            tags.put("EXCEEDING_TAG_KEY", valuesExceedingLimit);
            List<String> values = Arrays.asList("AAAHDBCN", "XBBCAUDH");
            tags.put(IUN, values);

            return tags;
        }

        public Mono<Map<String, List<String>>> generateRandomTags() {
            return Flux.range(0, 50)
                    .flatMap(index -> Mono.fromCallable(() -> generateRandomTagMap(index)))
                    .collectMap(map -> "RandomKey" + map.get("index"), map -> map.get("values"));
        }

        // Metodo per generare una mappa casuale di tag per un indice
        private Map<String, List<String>> generateRandomTagMap(int index) {
            Map<String, List<String>> tagMap = new HashMap<>();
            Random random = new Random();
            int numberOfValues = random.nextInt(5) + 1;
            List<String> values = new ArrayList<>();
            for (int i = 0; i < numberOfValues; i++) {
                values.add(generateRandomValue());
            }
            tagMap.put("index", Collections.singletonList(String.valueOf(index)));
            tagMap.put("values", values);
            return tagMap;
        }

        // Metodo per generare un valore casuale della mappa
        private String generateRandomValue() {
            int leftLimit = 97; // A
            int rightLimit = 122; // Z
            int targetStringLength = 8;
            Random random = new Random();
            return random.ints(leftLimit, rightLimit + 1)
                    .limit(targetStringLength)
                    .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                    .toString();
        }
    }
}
