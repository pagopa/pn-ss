package it.pagopa.pnss.uribuilder;

import com.amazonaws.SdkClientException;
import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.*;
import it.pagopa.pnss.common.DocTypesConstant;
import it.pagopa.pnss.common.client.DocTypesClientCall;
import it.pagopa.pnss.common.client.DocumentClientCall;
import it.pagopa.pnss.common.client.UserConfigurationClientCall;
import it.pagopa.pnss.common.client.exception.DocumentKeyNotPresentException;
import it.pagopa.pnss.common.client.exception.S3BucketException;
import it.pagopa.pnss.configurationproperties.BucketName;
import it.pagopa.pnss.testutils.annotation.SpringBootTestWebEnv;
import it.pagopa.pnss.transformation.service.S3Service;
import it.pagopa.pnss.uribuilder.service.UriBuilderService;
import lombok.CustomLog;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.model.*;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.URI;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static it.pagopa.pnss.common.DocTypesConstant.*;
import static it.pagopa.pnss.common.constant.Constant.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTestWebEnv
@CustomLog
@AutoConfigureWebTestClient(timeout = "36000")
class UriBuilderServiceDownloadTest {

    @Value("${header.x-api-key:#{null}}")
    private String xApiKey;
    @Value("${header.x-pagopa-safestorage-cx-id:#{null}}")
    private String X_PAGOPA_SAFESTORAGE_CX_ID;
    @Value("${queryParam.presignedUrl.traceId:#{null}}")
    private String queryParamPresignedUrlTraceId;

    @Value("${max.restore.time.cold}")
    BigDecimal maxRestoreTimeCold;

    private static final String X_API_KEY_VALUE = "apiKey_value";
    private static final String X_PAGO_PA_SAFESTORAGE_CX_ID_VALUE = "CLIENT_ID_123";
    private static final String X_QUERY_PARAM_URL_VALUE= "queryParamPresignedUrlTraceId_value";

    private static final UserConfigurationResponse USER_CONFIGURATION_RESPONSE =
            new UserConfigurationResponse().userConfiguration(new UserConfiguration().apiKey(X_API_KEY_VALUE));

    private final String PATTERN_FORMAT = "yyyy-MM-dd'T'HH:mm:ssXXX";
    private static final String SEPARATORE = "~";
    private static final String PREFIX = "CLIENT_ID" + SEPARATORE;

    private static final String IUN = "IUN";
    private final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern(PATTERN_FORMAT).withZone(ZoneId.from(ZoneOffset.UTC));

    @Value("${file.download.api.url}")
    public String urlDownload;

    @Autowired
    private WebTestClient webClient;

    @MockBean
    UserConfigurationClientCall userConfigurationClientCall;

    @MockBean
    DocumentClientCall documentClientCall;

    @MockBean
    DocTypesClientCall docTypesClientCall;

    @SpyBean
    S3Service s3Service;
    
    @SpyBean
    UriBuilderService uriBuilderService;

    @Value("${test.aws.s3.endpoint:#{null}}")
    String testAwsS3Endpoint;

    @Autowired
    BucketName bucketName;
    private static final String CHECKSUM = "91375e9e5a9510087606894437a6a382fa5bc74950f932e2b85a788303cf5ba0";

    @Value("${default.internal.x-api-key.value:#{null}}")
    private String defaultInternalApiKeyValue;

    @Value("${default.internal.header.x-pagopa-safestorage-cx-id:#{null}}")
    private String defaultInternalClientIdValue;

    private static final DocumentResponse DOCUMENT_RESPONSE_TAGS = new DocumentResponse().document(new Document().documentKey("documentKey").tags(createTagsList()).documentType(new DocumentType().checksum(DocumentType.ChecksumEnum.MD5)));


    private WebTestClient.RequestHeadersSpec callRequestHeadersSpec(String requestIdx, Boolean metadataOnly, String clientId, String apiKey, Boolean tags)
    {
        this.webClient.mutate().responseTimeout(Duration.ofMillis(30000)).build();
        return this.webClient.get()
                .uri(uriBuilder -> uriBuilder.path(urlDownload).queryParam("metadataOnly", metadataOnly)
                        .queryParam("tags", tags)
                        //... building a URI
                        .build(requestIdx))
                .header(X_PAGOPA_SAFESTORAGE_CX_ID, clientId)
                .header(xApiKey, apiKey)
                .header(HttpHeaders.ACCEPT, "application/json")
                .attribute("metadataOnly", metadataOnly);
    }

    private WebTestClient.ResponseSpec fileDownloadTestCall(String requestIdx, Boolean metadataOnly) {
        return callRequestHeadersSpec(requestIdx, metadataOnly, X_PAGO_PA_SAFESTORAGE_CX_ID_VALUE, X_API_KEY_VALUE, false)
                .header(queryParamPresignedUrlTraceId, X_QUERY_PARAM_URL_VALUE)
                .exchange();
    }

    private WebTestClient.ResponseSpec fileDownloadTestCallWithClientId(String requestIdx, Boolean metadataOnly, String clientId, String apiKey) {
        return callRequestHeadersSpec(requestIdx, metadataOnly, clientId, apiKey, true)
                .header(queryParamPresignedUrlTraceId, X_QUERY_PARAM_URL_VALUE)
                .exchange();
    }

    private WebTestClient.ResponseSpec noTraceIdFileDownloadTestCall(String requestIdx, Boolean metadataOnly) {
        return callRequestHeadersSpec(requestIdx, metadataOnly, X_PAGO_PA_SAFESTORAGE_CX_ID_VALUE, X_API_KEY_VALUE, false)
                .exchange();
    }


    private static Map<String, List<String>> createTagsList(){
        Map<String, List<String>> tags = new HashMap<>();

        List<String> valuesForKey1 = new ArrayList<>();
        valuesForKey1.add("AABBCC");
        valuesForKey1.add("DAHKLAO");
        tags.put(X_PAGO_PA_SAFESTORAGE_CX_ID_VALUE+SEPARATORE+IUN, valuesForKey1);

        List<String> valuesForKey2 = new ArrayList<>();
        valuesForKey2.add("BGDKAO");
        tags.put(X_PAGO_PA_SAFESTORAGE_CX_ID_VALUE+"_2"+SEPARATORE+IUN, valuesForKey2);

        List<String> valuesForKey3 = new ArrayList<>();
        valuesForKey3.add("UHANCLX");
        valuesForKey3.add("CNALUZX");
        valuesForKey3.add("DDEEFFGG");
        tags.put(X_PAGO_PA_SAFESTORAGE_CX_ID_VALUE+"_3"+SEPARATORE+IUN, valuesForKey3);

        List<String> valuesForKey4 = new ArrayList<>();
        valuesForKey4.add("BCAGIST");
        tags.put(X_PAGO_PA_SAFESTORAGE_CX_ID_VALUE+"_4"+SEPARATORE+IUN, valuesForKey4);

        List<String> valuesForKey5 = new ArrayList<>();
        valuesForKey5.add("XJSHAOP");
        tags.put("Conservazione", valuesForKey5);
        return tags;
    }

    @BeforeEach
    private void createUserConfiguration() {

        log.info("createUserConfiguration() : START");

        UserConfiguration userConfiguration = new UserConfiguration();
        userConfiguration.setName(X_PAGO_PA_SAFESTORAGE_CX_ID_VALUE);
        userConfiguration.setApiKey(X_API_KEY_VALUE);

        UserConfigurationResponse userConfig = new UserConfigurationResponse();
        userConfig.setUserConfiguration(userConfiguration);

        Mono<UserConfigurationResponse> userConfigurationResponse = Mono.just(userConfig);
        doReturn(userConfigurationResponse).when(userConfigurationClientCall).getUser(Mockito.any());

        log.info("createUserConfiguration() : END");
    }

    private String getDownloadFileEndpoint() {
        return urlDownload;
    }


    @Test
    void testMissingTraceIdHeader()
    {
        when(userConfigurationClientCall.getUser(anyString())).thenReturn(Mono.just(USER_CONFIGURATION_RESPONSE));

        String docId = "1111-aaaa";
        mockUserConfiguration(List.of(DocTypesConstant.PN_AAR));

        DocumentInput d = new DocumentInput();
        d.setDocumentType(DocTypesConstant.PN_AAR);
        d.setDocumentState(AVAILABLE);
        d.setCheckSum(CHECKSUM);

        mockGetDocument(d, docId);

        when(docTypesClientCall.getdocTypes(DocTypesConstant.PN_AAR)).thenReturn(Mono.just(new DocumentTypeResponse().docType(new DocumentType())));

        noTraceIdFileDownloadTestCall(docId, true).expectStatus().isBadRequest();
    }
    @Test
    void testUrlGenerato() {

        when(userConfigurationClientCall.getUser(anyString())).thenReturn(Mono.just(USER_CONFIGURATION_RESPONSE));

        String docId = "1111-aaaa";
        mockUserConfiguration(List.of(DocTypesConstant.PN_AAR));

        DocumentInput d = new DocumentInput();
        d.setDocumentType(DocTypesConstant.PN_AAR);
        d.setDocumentState(AVAILABLE);
        d.setCheckSum(CHECKSUM);

        mockGetDocument(d, docId);

        var now = Instant.now();

        when(documentClientCall.patchDocument(anyString(), anyString(), anyString(), any(DocumentChanges.class)))
                .thenReturn(Mono.just(new DocumentResponse().document(new Document().documentKey(docId))));
        when(docTypesClientCall.getdocTypes(DocTypesConstant.PN_AAR)).thenReturn(Mono.just(new DocumentTypeResponse().docType(new DocumentType())));
        when(s3Service.headObject(anyString(), anyString())).thenReturn(Mono.just(HeadObjectResponse.builder().objectLockRetainUntilDate(now).build()));
        when(documentClientCall.patchDocument(defaultInternalClientIdValue, defaultInternalApiKeyValue, docId, new DocumentChanges().retentionUntil(DATE_TIME_FORMATTER.format(now))))
                .thenReturn(Mono.just(new DocumentResponse().document(new Document().documentKey(docId))));


        fileDownloadTestCall(docId, false).expectStatus().isOk();
    }

//    @Test
//    void testUrlGeneratoConMetaDataTrue() {
//        String docId = "1111-aaaa";
//
//        mockUserConfiguration(List.of(DocTypesConstant.PN_AAR));
//
//
//        DocumentInput d = new DocumentInput();
//        d.setDocumentType(DocTypesConstant.PN_AAR);
//        d.setDocumentState(AVAILABLE);
//        d.setCheckSum(CHECKSUM);
//
//        mockGetDocument(d, docId);
//
//        when(docTypesClientCall.getdocTypes(DocTypesConstant.PN_AAR)).thenReturn(Mono.just(new DocumentTypeResponse().docType(new DocumentType())));
//        when(s3Service.headObject(anyString(), anyString())).thenReturn(Mono.just(HeadObjectResponse.builder().objectLockRetainUntilDate(Instant.now()).build()));
//        when(documentClientCall.patchDocument(anyString(), anyString(), anyString(), any(DocumentChanges.class))).thenReturn(Mono.just(new DocumentResponse().document(new Document().documentKey(docId))));
//
//
//        fileDownloadTestCall(docId, true).expectStatus().isOk();
//    }


//    @Test
//    void testUrlGeneratoConMetaDataNull() {
//        when(userConfigurationClientCall.getUser(anyString())).thenReturn(Mono.just(USER_CONFIGURATION_RESPONSE));
//
//        String docId = "1111-aaaa";
//
//        mockUserConfiguration(List.of(DocTypesConstant.PN_AAR));
//
//        DocumentInput d = new DocumentInput();
//        d.setDocumentType(DocTypesConstant.PN_AAR);
//        d.setDocumentState(AVAILABLE);
//        d.setCheckSum(CHECKSUM);
//
//        mockGetDocument(d, docId);
//
//        when(docTypesClientCall.getdocTypes(DocTypesConstant.PN_AAR)).thenReturn(Mono.just(new DocumentTypeResponse().docType(new DocumentType())));
//        when(s3Service.headObject(anyString(), anyString())).thenReturn(Mono.just(HeadObjectResponse.builder().objectLockRetainUntilDate(Instant.now()).build()));
//        when(documentClientCall.patchDocument(anyString(), anyString(), anyString(), any(DocumentChanges.class))).thenReturn(Mono.just(new DocumentResponse()));
//
//
//        fileDownloadTestCall(docId, true).expectStatus().isOk();
//    }

    @Test
    void createFileDownloadInfoOk() {

        RestoreObjectResponse restoreObjectResponse = RestoreObjectResponse.builder().build();
        when(s3Service.restoreObject(anyString(), anyString(), any(RestoreRequest.class))).thenReturn(Mono.just(restoreObjectResponse));

        var testMono = uriBuilderService.createFileDownloadInfo("fileKey", "xTraceIdValue", FREEZED, false);
        StepVerifier.create(testMono).expectNextCount(1).verifyComplete();
    }


    @Test
    void recoverDocumentFromBucketRestoreAlreadyInProgress() {
        String restoreRequestDate = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.ENGLISH)
                .withZone(ZoneId.of("GMT"))
                .format(OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(10));


        AwsErrorDetails awsErrorDetails = AwsErrorDetails.builder().errorCode("RestoreAlreadyInProgress").build();
        SdkHttpResponse sdkHttpResponse= SdkHttpResponse.builder().putHeader("x-amz-restore-request-date", List.of(restoreRequestDate)).build();
        HeadObjectResponse headObjectResponse = (HeadObjectResponse) HeadObjectResponse.builder().sdkHttpResponse(sdkHttpResponse).build();
        when(s3Service.headObject(anyString(), anyString())).thenReturn(Mono.just(headObjectResponse));
        when(s3Service.restoreObject(anyString(), anyString(), any(RestoreRequest.class))).thenReturn(Mono.error(AwsServiceException.builder().awsErrorDetails(awsErrorDetails).build()));

        var testMono = uriBuilderService.createFileDownloadInfo("fileKey", "xTraceIdValue", FREEZED, false);
        StepVerifier.create(testMono)
                .expectNextMatches(fileDownloadInfo -> fileDownloadInfo.getRetryAfter().compareTo(maxRestoreTimeCold) < 0)
                .verifyComplete();
    }

    @Test
    void recoverDocumentFromBucketRestoreAlreadyInProgressWithNegativeElapsedTime() {

        AwsErrorDetails awsErrorDetails = AwsErrorDetails.builder().errorCode("RestoreAlreadyInProgress").build();
        SdkHttpResponse sdkHttpResponse= SdkHttpResponse.builder().putHeader("x-amz-restore-request-date", List.of("Fri, 03 Nov 2023 21:21:21 GMT")).build();
        HeadObjectResponse headObjectResponse = (HeadObjectResponse) HeadObjectResponse.builder().sdkHttpResponse(sdkHttpResponse).build();
        when(s3Service.headObject(anyString(), anyString())).thenReturn(Mono.just(headObjectResponse));
        when(s3Service.restoreObject(anyString(), anyString(), any(RestoreRequest.class))).thenReturn(Mono.error(AwsServiceException.builder().awsErrorDetails(awsErrorDetails).build()));

        var testMono = uriBuilderService.createFileDownloadInfo("fileKey", "xTraceIdValue", FREEZED, false);
        StepVerifier.create(testMono)
                .expectNextMatches(fileDownloadInfo -> fileDownloadInfo.getRetryAfter().compareTo(BigDecimal.valueOf(3600)) == 0)
                .verifyComplete();    }

    @Test
    void recoverDocumentFromBucketNoSuchKey() {

        AwsErrorDetails awsErrorDetails = AwsErrorDetails.builder().errorCode("NoSuchKey").build();
        when(s3Service.restoreObject(anyString(), anyString(), any(RestoreRequest.class))).thenReturn(Mono.error(AwsServiceException.builder().awsErrorDetails(awsErrorDetails).build()));

        var testMono = uriBuilderService.createFileDownloadInfo("fileKey", "xTraceIdValue", FREEZED, false);
        StepVerifier.create(testMono).expectError(S3BucketException.NoSuchKeyException.class).verify();
    }

    @Test
    void recoverDocumentFromBucketError() {

        AwsErrorDetails awsErrorDetails = AwsErrorDetails.builder().errorCode("Errore Generico").build();
        when(s3Service.restoreObject(anyString(), anyString(), any(RestoreRequest.class))).thenReturn(Mono.error(AwsServiceException.builder().awsErrorDetails(awsErrorDetails).statusCode(500).build()));

        var testMono = uriBuilderService.createFileDownloadInfo("fileKey", "xTraceIdValue", FREEZED, false);
        StepVerifier.create(testMono).expectError(ResponseStatusException.class).verify();
    }

    @Test
    void recoverDocumentFromBucketSdkClientException() {

        SdkClientException sdkClientException = new SdkClientException("SdkClientException");
        when(s3Service.restoreObject(anyString(), anyString(), any(RestoreRequest.class))).thenReturn(Mono.error(sdkClientException));

        var testMono = uriBuilderService.createFileDownloadInfo("fileKey", "xTraceIdValue", FREEZED, false);
        StepVerifier.create(testMono).expectError(ResponseStatusException.class).verify();
    }

    @Test
    void getPresignedUrlNoSuchKey() {

        AwsErrorDetails awsErrorDetails = AwsErrorDetails.builder().errorCode("NoSuchKey").build();
        Mockito.doReturn(Mono.error(S3Exception.builder().awsErrorDetails(awsErrorDetails).build())).when(s3Service).presignGetObject(any(GetObjectRequest.class), any(Duration.class));

        var testMono = uriBuilderService.createFileDownloadInfo("fileKey", "xTraceIdValue", AVAILABLE, false);
        StepVerifier.create(testMono).expectError(S3BucketException.NoSuchKeyException.class).verify();
    }

    @Test
    void getPresignedUrlError() {

        AwsErrorDetails awsErrorDetails = AwsErrorDetails.builder().errorCode("Errore Generico").errorMessage("Errore Generico").build();
        Mockito.doReturn(Mono.error(S3Exception.builder().awsErrorDetails(awsErrorDetails).statusCode(500).build())).when(s3Service).presignGetObject(any(GetObjectRequest.class), any(Duration.class));

        var testMono = uriBuilderService.createFileDownloadInfo("fileKey", "xTraceIdValue", AVAILABLE, false);
        StepVerifier.create(testMono).expectError(ResponseStatusException.class).verify();
    }

    @Test
    void getPresignedUrlSdkClientException() {

        SdkClientException sdkClientException = new SdkClientException("SdkClientException");
        Mockito.doReturn(Mono.error(sdkClientException)).when(s3Service).presignGetObject(any(GetObjectRequest.class), any(Duration.class));

        var testMono = uriBuilderService.createFileDownloadInfo("fileKey", "xTraceIdValue", AVAILABLE, false);
        StepVerifier.create(testMono).expectError(ResponseStatusException.class).verify();
    }

//    @Test
//    void testFileTrovatoBasketHot(){
//        String docId = "1111-aaaa";
//        mockUserConfiguration(List.of(DocTypesConstant.PN_AAR));
//
//        DocumentInput d = new DocumentInput();
//        d.setDocumentType(DocTypesConstant.PN_AAR);
//        d.setDocumentState(AVAILABLE);
//        d.setCheckSum("");
//        mockGetDocument(d, docId);
//        //Mockito.doReturn(fdr).when(service).createUriForDownloadFile(Mockito.any(), Mockito.any());
//        when(docTypesClientCall.getdocTypes(DocTypesConstant.PN_AAR)).thenReturn(Mono.just(new DocumentTypeResponse().docType(new DocumentType())));
//        when(s3Service.headObject(anyString(), anyString())).thenReturn(Mono.just(HeadObjectResponse.builder().objectLockRetainUntilDate(Instant.now()).build()));
//        when(documentClientCall.patchDocument(anyString(), anyString(), anyString(), any(DocumentChanges.class))).thenReturn(Mono.just(new DocumentResponse()));
//
//        fileDownloadTestCall( docId,false).expectStatus()
//                .isOk().expectBody(FileDownloadResponse.class).value(response ->{
//                    Assertions.assertThat(StringUtils.isNotEmpty(response.getDownload().getUrl()));
//                });
//    }


    @Test
    void testUrlStatusDeleted() {

        when(userConfigurationClientCall.getUser(anyString())).thenReturn(Mono.just(USER_CONFIGURATION_RESPONSE));

        String docId = "1111-aaaa";
        mockUserConfiguration(List.of(DocTypesConstant.PN_AAR));

        DocumentInput d = new DocumentInput();
        d.setDocumentType(DocTypesConstant.PN_AAR);
        d.setDocumentState(DELETED);
        d.setCheckSum(CHECKSUM);

        mockGetDocument(d, docId);

        when(docTypesClientCall.getdocTypes(DocTypesConstant.PN_AAR)).thenReturn(Mono.just(new DocumentTypeResponse().docType(new DocumentType())));

        fileDownloadTestCall(docId, true).expectStatus().isEqualTo(HttpStatus.GONE);
    }

//    @Test
//    void testDocumentMissingFromBucket() {
//
//        doThrow(new S3BucketException.NoSuchKeyException("")).when(uriBuilderService).createFileDownloadInfo(any(), any(), any(), anyBoolean());
//
//        when(userConfigurationClientCall.getUser(anyString())).thenReturn(Mono.just(USER_CONFIGURATION_RESPONSE));
//
//    	String docId = "1111-aaaa"; mockUserConfiguration(List.of(DocTypesConstant.PN_AAR));
//    	DocumentInput d = new DocumentInput(); d.setDocumentType(DocTypesConstant.PN_AAR);
//    	d.setDocumentState(TECHNICAL_STATUS_BOOKED);
//
//    	when(docTypesClientCall.getdocTypes(DocTypesConstant.PN_AAR)).thenReturn(Mono.just(new DocumentTypeResponse().docType(new DocumentType())));
//
//        HeadObjectResponse headObjectResponse =  HeadObjectResponse.builder()
//                .sseCustomerKeyMD5("keymd5")
//                .checksumSHA256("sha256")
//                .contentLength(3L)
//                .objectLockRetainUntilDate(Instant.now())
//                .build();
//        when(s3Service.headObject(anyString(), anyString())).thenReturn(Mono.just(headObjectResponse));
//
//    	mockGetDocument(d, docId);
//    	fileDownloadTestCall(docId, false).expectStatus().isEqualTo(HttpStatus.NOT_FOUND);
//    }

    @Test
    void testDocumentMissingFromBucketStaged() {

        doThrow(new S3BucketException.NoSuchKeyException("")).when(uriBuilderService).createFileDownloadInfo(any(), any(), any(), anyBoolean());

        when(userConfigurationClientCall.getUser(anyString())).thenReturn(Mono.just(USER_CONFIGURATION_RESPONSE));

        String docId = "1111-aaaa"; mockUserConfiguration(List.of(DocTypesConstant.PN_AAR));
        DocumentInput d = new DocumentInput(); d.setDocumentType(DocTypesConstant.PN_AAR);
        d.setDocumentState(STAGED);

        when(docTypesClientCall.getdocTypes(DocTypesConstant.PN_AAR)).thenReturn(Mono.just(new DocumentTypeResponse().docType(new DocumentType())));

        mockGetDocument(d, docId);
        fileDownloadTestCall(docId, false).expectStatus().isEqualTo(HttpStatus.NOT_FOUND);
    }

//    @Test void testDocumentMissingFromBucketFreezed() {
//
//        doThrow(new S3BucketException.NoSuchKeyException("")).when(uriBuilderService).createFileDownloadInfo(any(), any(), any(), anyBoolean());
//
//        when(userConfigurationClientCall.getUser(anyString())).thenReturn(Mono.just(USER_CONFIGURATION_RESPONSE));
//
//        String docId = "1111-aaaa"; mockUserConfiguration(List.of(DocTypesConstant.PN_AAR));
//        DocumentInput d = new DocumentInput(); d.setDocumentType(DocTypesConstant.PN_AAR);
//        d.setDocumentState(TECHNICAL_STATUS_FREEZED);
//
//        when(docTypesClientCall.getdocTypes(DocTypesConstant.PN_AAR)).thenReturn(Mono.just(new DocumentTypeResponse().docType(new DocumentType())));
//
//        mockGetDocument(d, docId);
//        fileDownloadTestCall(docId, false).expectStatus().isEqualTo(HttpStatus.NOT_FOUND);
//    }

   /* @Test
    void testFileTrovatoBasketCold() {
        String docId = "1111-aaaa";
        mockUserConfiguration(List.of(PN_AAR));


        DocumentInput d = new DocumentInput();
        d.setDocumentType(PN_AAR);
        d.setDocumentState(FREEZED);
        d.setDocumentLogicalState(SAVED);
        d.setCheckSum("" );
        mockGetDocument(d, docId);
        addFileToBucket(docId);
        //Mockito.doReturn(fdr).when(service).createUriForDownloadFile(Mockito.any(), Mockito.any());
        when(docTypesClientCall.getdocTypes(PN_AAR)).thenReturn(Mono.just(new DocumentTypeResponse().docType(new DocumentType())));

        fileDownloadTestCall( docId,false).expectStatus()
                .isOk().expectBody(FileDownloadResponse.class).value(response ->{
                    //Assertions.assertThat(!response.getChecksum().isEmpty());
                    //TODO rimettere
                    Assertions.assertThat(!response.getDownload().getRetryAfter().equals(maxRestoreTimeCold));

                });
    }*/

    @Test
    void testFileNonTrovato() {

        String docId = "1111-aaaa";
        DocumentInput d = new DocumentInput();
        d.setDocumentType(DocTypesConstant.PN_AAR);
        mockUserConfiguration(List.of(DocTypesConstant.PN_AAR));
        mockGetDocument(d, docId);
        when(userConfigurationClientCall.getUser(anyString())).thenReturn(Mono.just(USER_CONFIGURATION_RESPONSE));
        Mockito.when(documentClientCall.getDocument(Mockito.any())).thenReturn(Mono.error(new DocumentKeyNotPresentException("keyFile")));
        fileDownloadTestCall(docId, null).expectStatus().isNotFound();

    }

    @Test
    void testIdClienteNonTrovatoDownload() {

        Mockito.doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "User Not Found : "))
                .when(userConfigurationClientCall)
                .getUser(Mockito.any());
        String docId = "1111-aaaa";
        fileDownloadTestCall(docId, null).expectStatus().isNotFound();

    }

    @Test
    void testIdClienteNoPermessiDownload() {
        when(userConfigurationClientCall.getUser(anyString())).thenReturn(Mono.just(USER_CONFIGURATION_RESPONSE));

        String docId = "1111-aaaa";

        mockUserConfiguration(List.of(DocTypesConstant.PN_NOTIFICATION_ATTACHMENTS));

        DocumentInput d = new DocumentInput();
        d.setDocumentType(DocTypesConstant.PN_AAR);
        d.setDocumentState(TECHNICAL_STATUS_AVAILABLE);

        mockGetDocument(d, docId);
        fileDownloadTestCall(docId, false).expectStatus().isForbidden();
    }

    @Test
    void testCreateUriForDownloadFileTags() {
        // Inizializzazione dei parametri di input per il metodo da testare
     //   String fileKey = "fileKey";
        String xPagopaSafestorageCxId = "xPagopaSafestorageCxId";
        String xTraceIdValue = "xTraceIdValue";
        Boolean metadataOnly = false;

        // Mock delle risposte delle chiamate interne
        UserConfigurationResponse userConfig = new UserConfigurationResponse();
        UserConfiguration userConfiguration = new UserConfiguration();
        userConfiguration.setName(X_PAGO_PA_SAFESTORAGE_CX_ID_VALUE);
        userConfiguration.setApiKey(X_API_KEY_VALUE);
        userConfiguration.setCanReadTags(true);
        userConfiguration.setCanRead(Collections.singletonList(PN_AAR));
        userConfig.setUserConfiguration(userConfiguration);

        DocumentInput d = new DocumentInput();
        d.setDocumentType(DocTypesConstant.PN_AAR);
        d.setDocumentState(TECHNICAL_STATUS_AVAILABLE);
        d.setDocumentKey("documentKey");

        when(userConfigurationClientCall.getUser(anyString())).thenReturn(Mono.just(userConfig));
        mockGetDocumentTags(d, "documentKey");
        when(docTypesClientCall.getdocTypes(anyString())).thenReturn(Mono.just(new DocumentTypeResponse().docType(new DocumentType().transformations(List.of(DocumentType.TransformationsEnum.SIGN_AND_TIMEMARK)))));
        // Chiamata al metodo da testare
        Mono<FileDownloadResponse> result = uriBuilderService.createUriForDownloadFile("documentKey", xPagopaSafestorageCxId, xTraceIdValue, metadataOnly, true);

        // Verifica del risultato
        StepVerifier.create(result)
                .expectNextMatches(Objects::nonNull)
                .verifyComplete();

        // Verifica che il metodo mock sia stato chiamato con i parametri corretti
        verify(documentClientCall, times(2)).getDocument("documentKey");

    }

    /**
     * Test con queryParam 'tags' valorizzato a 'false'.
     * Restityisce una response priva di informazione sui tag
     */
    @Test
    void testCreateUriForDownloadFileTags_tagFalse() {
        // Inizializzazione dei parametri di input per il metodo da testare
        //   String fileKey = "fileKey";
        String xPagopaSafestorageCxId = "xPagopaSafestorageCxId";
        String xTraceIdValue = "xTraceIdValue";
        Boolean metadataOnly = false;

        // Mock delle risposte delle chiamate interne
        UserConfigurationResponse userConfig = new UserConfigurationResponse();
        UserConfiguration userConfiguration = new UserConfiguration();
        userConfiguration.setName(X_PAGO_PA_SAFESTORAGE_CX_ID_VALUE);
        userConfiguration.setApiKey(X_API_KEY_VALUE);
        userConfiguration.setCanReadTags(true);
        userConfiguration.setCanRead(Collections.singletonList(PN_AAR));
        userConfig.setUserConfiguration(userConfiguration);

        DocumentInput d = new DocumentInput();
        d.setDocumentType(DocTypesConstant.PN_AAR);
        d.setDocumentState(TECHNICAL_STATUS_AVAILABLE);
        d.setDocumentKey("documentKey");

        when(userConfigurationClientCall.getUser(anyString())).thenReturn(Mono.just(userConfig));
        mockGetDocumentTags(d, "documentKey");
        when(docTypesClientCall.getdocTypes(anyString())).thenReturn(Mono.just(new DocumentTypeResponse().docType(new DocumentType().transformations(List.of(DocumentType.TransformationsEnum.SIGN_AND_TIMEMARK)))));
        // Chiamata al metodo da testare
        Mono<FileDownloadResponse> result = uriBuilderService.createUriForDownloadFile("documentKey", xPagopaSafestorageCxId, xTraceIdValue, metadataOnly, false);

        // Verifica del risultato
        StepVerifier.create(result)
                .expectNextMatches(Objects::nonNull)
                .verifyComplete();

        // Verifica che il metodo mock sia stato chiamato con i parametri corretti
        verify(documentClientCall, times(1)).getDocument("documentKey");

    }

    @ParameterizedTest
    @NullSource
    @ValueSource(booleans = {false})
    void testCreateUriForDownloadFileTags_noPermessi_Ko(Boolean canReadTags) {
        log.info("CanReadTags: " + canReadTags);
        String docId = "documentKey";
        String xPagopaSafestorageCxId = "client-not-authorized";
        String xApiKey = "client-not-authorized_api_key";

        // Mock delle risposte delle chiamate interne
        UserConfigurationResponse userConfig = new UserConfigurationResponse();
        UserConfiguration userConfiguration = new UserConfiguration();
        userConfiguration.setName(xPagopaSafestorageCxId);
        userConfiguration.setApiKey(xApiKey);
        userConfiguration.setCanReadTags(canReadTags);
        userConfiguration.setCanRead(Collections.singletonList(PN_AAR));
        userConfig.setUserConfiguration(userConfiguration);

        DocumentInput d = new DocumentInput();
        d.setDocumentType(DocTypesConstant.PN_AAR);
        d.setDocumentState(TECHNICAL_STATUS_AVAILABLE);

        when(userConfigurationClientCall.getUser(anyString())).thenReturn(Mono.just(userConfig));
        mockGetDocument(d, docId);

        // Chiamata al metodo da testare
        fileDownloadTestCallWithClientId(docId, false, xPagopaSafestorageCxId, xApiKey).expectStatus().isForbidden();
    }

    private void mockGetDocument(DocumentInput d, String docId) {
        DocumentResponse documentResponse = new DocumentResponse();
        Document doc = new Document();
        DocumentType type = new DocumentType();
        type.setTipoDocumento(d.getDocumentType());
        type.setChecksum(DocumentType.ChecksumEnum.MD5);
        type.setStatuses(Map.of(SAVED, new CurrentStatus().technicalState(AVAILABLE)));
        doc.setDocumentType(type);
        doc.setDocumentState(d.getDocumentState());
        doc.setDocumentLogicalState(d.getDocumentLogicalState());
        doc.setRetentionUntil(OffsetDateTime.now().format(DATE_TIME_FORMATTER));
        documentResponse.setDocument(doc);
        Mono<DocumentResponse> docRespEntity = Mono.just(documentResponse);
        doReturn(docRespEntity).when(documentClientCall).getDocument(docId);
    }   private void mockGetDocumentTags(DocumentInput d, String docId) {
        DocumentResponse documentResponse = new DocumentResponse();
        Document doc = new Document();
        DocumentType type = new DocumentType();
        type.setTipoDocumento(d.getDocumentType());
        type.setChecksum(DocumentType.ChecksumEnum.MD5);
        type.setStatuses(Map.of(SAVED, new CurrentStatus().technicalState(AVAILABLE)));
        doc.setDocumentType(type);
        doc.setDocumentState(d.getDocumentState());
        doc.setDocumentLogicalState(d.getDocumentLogicalState());
        doc.setRetentionUntil(OffsetDateTime.now().format(DATE_TIME_FORMATTER));
        doc.setTags(createTagsList());
        doc.setDocumentKey(docId);
        documentResponse.setDocument(doc);
        Mono<DocumentResponse> docRespEntity = Mono.just(documentResponse);
        doReturn(docRespEntity).when(documentClientCall).getDocument(docId);
    }

    private void mockUserConfiguration(List<String> permessi) {
        when(userConfigurationClientCall.getUser(anyString())).thenReturn(Mono.just(new UserConfigurationResponse().userConfiguration(new UserConfiguration().canRead(
                permessi).apiKey(X_API_KEY_VALUE))));
    }
    private void addFileToBucket(String fileName) {
        S3ClientBuilder client = S3Client.builder();
        client.endpointOverride(URI.create(testAwsS3Endpoint));
        S3Client s3Client = client.build();
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucketName.ssHotName())
                .storageClass(StorageClass.GLACIER)
                .key(fileName).build();

        s3Client.putObject(request, RequestBody.fromBytes(readPdfDocoument()));
    }

    private byte[] readPdfDocoument() {
        byte[] byteArray=null;
        try {
            InputStream is =  getClass().getResourceAsStream("/PDF_PROVA.pdf");
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();

            int nRead;
            byte[] data = new byte[16384];

            while ((nRead = is.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, nRead);
            }

            byteArray = buffer.toByteArray();

        } catch (FileNotFoundException e) {
            System.out.println("File Not found"+e);
        } catch (IOException e) {
            System.out.println("IO Ex"+e);
        }
        return byteArray;

    }

}
