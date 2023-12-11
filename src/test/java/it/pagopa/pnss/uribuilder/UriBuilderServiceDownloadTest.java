package it.pagopa.pnss.uribuilder;

import com.amazonaws.SdkClientException;
import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.DocumentInput;
import it.pagopa.pnss.common.model.dto.MacchinaStatiValidateStatoResponseDto;
import it.pagopa.pnss.common.model.pojo.DocumentStatusChange;
import it.pagopa.pnss.common.rest.call.machinestate.CallMacchinaStati;
import it.pagopa.pnss.configurationproperties.BucketName;
import it.pagopa.pnss.configurationproperties.RepositoryManagerDynamoTableName;
import it.pagopa.pnss.repositorymanager.entity.CurrentStatusEntity;
import it.pagopa.pnss.repositorymanager.entity.DocTypeEntity;
import it.pagopa.pnss.repositorymanager.entity.DocumentEntity;
import it.pagopa.pnss.repositorymanager.entity.UserConfigurationEntity;
import it.pagopa.pnss.repositorymanager.service.DocumentService;
import it.pagopa.pnss.testutils.annotation.SpringBootTestWebEnv;
import it.pagopa.pnss.transformation.service.S3Service;
import lombok.CustomLog;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.jupiter.api.*;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.time.Duration;
import java.util.Map;

import static it.pagopa.pnss.common.DocTypesConstant.PN_AAR;
import static it.pagopa.pnss.common.UserConfigurationConstant.PN_DELIVERY;
import static it.pagopa.pnss.common.UserConfigurationConstant.PN_TEST;
import static it.pagopa.pnss.common.constant.Constant.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTestWebEnv
@AutoConfigureWebTestClient
@CustomLog
class UriBuilderServiceDownloadTest {

    @Value("${header.x-api-key:#{null}}")
    private String xApiKey;
    @Value("${header.x-pagopa-safestorage-cx-id:#{null}}")
    private String X_PAGOPA_SAFESTORAGE_CX_ID;
    @Value("${queryParam.presignedUrl.traceId:#{null}}")
    private String queryParamPresignedUrlTraceId;
    @Value("${file.download.api.url}")
    public String urlDownload;
    @Autowired
    private WebTestClient webClient;
    @Autowired
    BucketName bucketName;
    @Autowired
    S3Client s3TestClient;
    @Autowired
    DocumentService documentService;
    @MockBean
    CallMacchinaStati callMacchinaStati;
    @SpyBean
    S3Service s3Service;
    private static final String X_API_KEY_VALUE = "pn-test_api_key";
    private static final String X_QUERY_PARAM_URL_VALUE = "queryParamPresignedUrlTraceId_value";
    private static final String DOCUMENT_KEY_AVAILABLE = "DOCUMENT_KEY_AVAILABLE";
    private static final String DOCUMENT_KEY_FREEZED = "DOCUMENT_KEY_FREEZED";
    private static final String DOCUMENT_KEY_BOOKED = "DOCUMENT_KEY_BOOKED";
    private static final String DOCUMENT_KEY_DELETED = "DOCUMENT_KEY_DELETED";
    private static final String DOCUMENT_KEY_STAGED = "DOCUMENT_KEY_STAGED";
    private static final String DOCUMENT_KEY_RETENTION_NULL = "DOCUMENT_KEY_RETENTION_NULL";
    private static final String RETENTION_UNTIL = "2023-09-07T17:34:15+02:00";

    private WebTestClient.RequestHeadersSpec callRequestHeadersSpec(String requestIdx, String clientId, String apiKey, Boolean metadataOnly) {
        this.webClient.mutate().responseTimeout(Duration.ofMillis(30000)).build();
        return this.webClient.get()
                .uri(uriBuilder -> uriBuilder.path(urlDownload).queryParam("metadataOnly", metadataOnly).build(requestIdx))
                .header(X_PAGOPA_SAFESTORAGE_CX_ID, clientId)
                .header(xApiKey, apiKey)
                .header(HttpHeaders.ACCEPT, "application/json")
                .attribute("metadataOnly", metadataOnly);
    }

    private WebTestClient.ResponseSpec fileDownloadTestCall(String requestIdx, Boolean metadataOnly) {
        return callRequestHeadersSpec(requestIdx, PN_TEST, X_API_KEY_VALUE, metadataOnly)
                .header(queryParamPresignedUrlTraceId, X_QUERY_PARAM_URL_VALUE)
                .exchange();
    }

    private WebTestClient.ResponseSpec noTraceIdFileDownloadTestCall(String requestIdx, Boolean metadataOnly) {
        return callRequestHeadersSpec(requestIdx, PN_TEST, X_API_KEY_VALUE, metadataOnly)
                .exchange();
    }

    private static DynamoDbTable<DocumentEntity> documentEntityDynamoDbTable;

    @BeforeAll
    public static void init(@Autowired DynamoDbEnhancedClient dynamoDbEnhancedClient, @Autowired RepositoryManagerDynamoTableName gestoreRepositoryDynamoDbTableName) {
        documentEntityDynamoDbTable = dynamoDbEnhancedClient.table(gestoreRepositoryDynamoDbTableName.documentiName(), TableSchema.fromBean(DocumentEntity.class));

        insertDocument(DOCUMENT_KEY_AVAILABLE, AVAILABLE, RETENTION_UNTIL);
        insertDocument(DOCUMENT_KEY_FREEZED, FREEZED, RETENTION_UNTIL);
        insertDocument(DOCUMENT_KEY_DELETED, DELETED, RETENTION_UNTIL);
        insertDocument(DOCUMENT_KEY_STAGED, STAGED, RETENTION_UNTIL);
        insertDocument(DOCUMENT_KEY_RETENTION_NULL, AVAILABLE, null);
    }

    private static void insertDocument(String documentKey, String documentState, String retentionUntil) {
        DocumentEntity documentEntity = new DocumentEntity();
        DocTypeEntity type = new DocTypeEntity();

        CurrentStatusEntity currentStatusEntity = new CurrentStatusEntity();
        currentStatusEntity.setStorage(STORAGE_TYPE);
        currentStatusEntity.setTechnicalState(AVAILABLE);
        type.setTipoDocumento(PN_AAR);
        type.setChecksum("MD5");
        type.setStatuses(Map.of(SAVED, currentStatusEntity));

        documentEntity.setDocumentKey(documentKey);
        documentEntity.setDocumentType(type);
        documentEntity.setDocumentState(documentState);
        documentEntity.setRetentionUntil(retentionUntil);
        documentEntityDynamoDbTable.putItem(documentEntity);
    }

    @Nested
    class StatusAvailable {
        @Test
        void testMissingTraceIdHeader() {
            noTraceIdFileDownloadTestCall(DOCUMENT_KEY_AVAILABLE, true).expectStatus().isBadRequest();
        }

        @Test
        void testUrlGeneratoAvailableOk() {
            s3Service.putObject(DOCUMENT_KEY_AVAILABLE, new byte[10], bucketName.ssHotName()).block();
            fileDownloadTestCall(DOCUMENT_KEY_AVAILABLE, false).expectStatus().isOk();
            deleteObjectFromBucket(DOCUMENT_KEY_AVAILABLE);
        }

        @Test
        void testUrlGeneratoAvailableNotInBucket() {
            AwsErrorDetails awsErrorDetails = AwsErrorDetails.builder().errorCode("NoSuchKey").build();
            Mockito.doReturn(Mono.error(S3Exception.builder().awsErrorDetails(awsErrorDetails).build())).when(s3Service).presignGetObject(any(GetObjectRequest.class), any(Duration.class));
            fileDownloadTestCall(DOCUMENT_KEY_AVAILABLE, false).expectStatus().isNotFound();
        }

        @Test
        void testUrlGeneratoGenericS3Exception() {
            AwsErrorDetails awsErrorDetails = AwsErrorDetails.builder().errorCode("Error").build();
            Mockito.doReturn(Mono.error(S3Exception.builder().awsErrorDetails(awsErrorDetails).statusCode(403).build())).when(s3Service).presignGetObject(any(GetObjectRequest.class), any(Duration.class));
            fileDownloadTestCall(DOCUMENT_KEY_AVAILABLE, false).expectStatus().isForbidden();
        }

        @Test
        void testUrlGeneratoAvailableSdkClientException() {
            Mockito.doReturn(Mono.error(new SdkClientException("Error"))).when(s3Service).presignGetObject(any(GetObjectRequest.class), any(Duration.class));
            fileDownloadTestCall(DOCUMENT_KEY_AVAILABLE, false).expectStatus().is5xxServerError();
        }

        @Test
        void testUrlGeneratoInvalidClientId() {
            callRequestHeadersSpec(DOCUMENT_KEY_AVAILABLE, "INVALID_CLIENT_ID", X_API_KEY_VALUE, false)
                    .header(queryParamPresignedUrlTraceId, X_QUERY_PARAM_URL_VALUE)
                    .exchange().expectStatus().isForbidden();
        }

        @Test
        void testUrlGeneratoInvalidApiKey() {
            callRequestHeadersSpec(DOCUMENT_KEY_AVAILABLE, PN_TEST, "INVALID_API_KEY", false)
                    .header(queryParamPresignedUrlTraceId, X_QUERY_PARAM_URL_VALUE)
                    .exchange().expectStatus().isForbidden();
        }

        @Test
        void testUrlGeneratoUnauthorizedClientId() {
            callRequestHeadersSpec(DOCUMENT_KEY_AVAILABLE, PN_DELIVERY, X_API_KEY_VALUE, false)
                    .header(queryParamPresignedUrlTraceId, X_QUERY_PARAM_URL_VALUE)
                    .exchange().expectStatus().isForbidden();
        }

        @Test
        void testUrlGeneratoMetadataOnlyOk() {
            fileDownloadTestCall(DOCUMENT_KEY_AVAILABLE, true).expectStatus().isOk();
        }

        @Test
        void testUrlGeneratoAvailableRetentionNull() {
            s3Service.putObject(DOCUMENT_KEY_RETENTION_NULL, new byte[10], bucketName.ssHotName()).block();
            fileDownloadTestCall(DOCUMENT_KEY_RETENTION_NULL, false).expectStatus().isOk();
            deleteObjectFromBucket(DOCUMENT_KEY_RETENTION_NULL);
        }

    }

    @Nested
    class StatusBooked {
        @BeforeEach
        void beforeEach() {
            documentService.insertDocument(new DocumentInput().documentKey(DOCUMENT_KEY_BOOKED).documentState(BOOKED).documentType(PN_AAR).retentionUntil(RETENTION_UNTIL)).block();
        }

        @AfterEach
        void afterEach() {
            documentService.deleteDocument(DOCUMENT_KEY_BOOKED).block();
        }

        @Test
        void testUrlGeneratoBookedOk() {
            s3Service.putObject(DOCUMENT_KEY_BOOKED, new byte[10], bucketName.ssHotName()).block();
            when(callMacchinaStati.statusValidation(any(DocumentStatusChange.class))).thenReturn(Mono.just(new MacchinaStatiValidateStatoResponseDto()));
            fileDownloadTestCall(DOCUMENT_KEY_BOOKED, false).expectStatus().isOk();
            s3Service.deleteObject(DOCUMENT_KEY_BOOKED, bucketName.ssHotName()).block();
        }

        @Test
        void testUrlGeneratoBookedNotInBucket() {
            when(callMacchinaStati.statusValidation(any(DocumentStatusChange.class))).thenReturn(Mono.just(new MacchinaStatiValidateStatoResponseDto()));
            fileDownloadTestCall(DOCUMENT_KEY_BOOKED, false).expectStatus().isNotFound();
        }
    }

    @Nested
    class StatusFreezed {
        @Test
        void testUrlGeneratoFreezedOk() {
            putObjectInBucket(DOCUMENT_KEY_FREEZED, StorageClass.GLACIER);
            fileDownloadTestCall(DOCUMENT_KEY_FREEZED, false).expectStatus().isOk();
            deleteObjectFromBucket(DOCUMENT_KEY_FREEZED);
        }

        //In localstack l'eccezione con il messaggio "RestoreAlreadyInProgress" non viene lanciata. La chiamata restoreObject deve quindi essere mockata.
        @Test
        void testUrlGeneratoFreezedRestoreAlreadyInProgress() {
            AwsErrorDetails awsErrorDetails = AwsErrorDetails.builder().errorCode("RestoreAlreadyInProgress").build();
            when(s3Service.restoreObject(anyString(), anyString(), any(RestoreRequest.class))).thenReturn(Mono.error(AwsServiceException.builder().awsErrorDetails(awsErrorDetails).build()));
            fileDownloadTestCall(DOCUMENT_KEY_FREEZED, false).expectStatus().isOk();
        }

        //In localstack l'eccezione con il messaggio "NoSuchKey" non viene lanciata. La chiamata restoreObject deve quindi essere mockata.
        @Test
        void testUrlGeneratoFreezedNotInBucket() {
            AwsErrorDetails awsErrorDetails = AwsErrorDetails.builder().errorCode("NoSuchKey").build();
            when(s3Service.restoreObject(anyString(), anyString(), any(RestoreRequest.class))).thenReturn(Mono.error(AwsServiceException.builder().awsErrorDetails(awsErrorDetails).build()));
            fileDownloadTestCall(DOCUMENT_KEY_FREEZED, false).expectStatus().isNotFound();
        }

        @Test
        void testUrlGeneratoFreezedGenericAwsException() {
            AwsErrorDetails awsErrorDetails = AwsErrorDetails.builder().errorCode("Error").build();
            when(s3Service.restoreObject(anyString(), anyString(), any(RestoreRequest.class))).thenReturn(Mono.error(AwsServiceException.builder().awsErrorDetails(awsErrorDetails).statusCode(403).build()));
            fileDownloadTestCall(DOCUMENT_KEY_FREEZED, false).expectStatus().isForbidden();
        }

        @Test
        void testUrlGeneratoSdkClientException() {
            when(s3Service.restoreObject(anyString(), anyString(), any(RestoreRequest.class))).thenReturn(Mono.error(new SdkClientException("Error")));
            fileDownloadTestCall(DOCUMENT_KEY_FREEZED, false).expectStatus().is5xxServerError();
        }
    }

    @Test
    void testUrlGeneratoDeleted() {
        fileDownloadTestCall(DOCUMENT_KEY_DELETED, true).expectStatus().isEqualTo(HttpStatus.GONE);
    }

    @Test
    void testUrlGeneratoStaged() {
        fileDownloadTestCall(DOCUMENT_KEY_STAGED, true).expectStatus().isNotFound();
    }

    @Test
    void testUrlGeneratoDocumentMissingFromDynamoDb() {
        fileDownloadTestCall("DOCUMENT_KEY_MISSING", null).expectStatus().isNotFound();
    }

    private void putObjectInBucket(String fileName, StorageClass storageClass) {
        byte[] fileBytes = new byte[10];
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucketName.ssHotName())
                .storageClass(storageClass)
                .key(fileName)
                .contentMD5(new String(Base64.encodeBase64(DigestUtils.md5(fileBytes))))
                .build();
        s3TestClient.putObject(request, RequestBody.fromBytes(fileBytes));
    }

    private void deleteObjectFromBucket(String fileName) {
        s3TestClient.deleteObject(builder -> builder.bucket(bucketName.ssHotName()).key(fileName));
    }

    private void restoreObjectInGlacier(String fileKey) {
        var restoreRequest = RestoreRequest.builder()
                .days(2)
                .glacierJobParameters(builder -> builder.tier(Tier.STANDARD))
                .build();
        s3TestClient.restoreObject(builder -> builder.key(fileKey).bucket(bucketName.ssHotName()).restoreRequest(restoreRequest));
    }

}
