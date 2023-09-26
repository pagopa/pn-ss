package it.pagopa.pnss.common.retention;

import it.pagopa.pn.template.internal.rest.v1.dto.DocumentChanges;
import it.pagopa.pn.template.rest.v1.dto.DocumentTypeConfiguration;
import it.pagopa.pn.template.rest.v1.dto.DocumentTypeConfigurationStatuses;
import it.pagopa.pn.template.rest.v1.dto.DocumentTypesConfigurations;
import it.pagopa.pn.template.rest.v1.dto.StorageConfiguration;
import it.pagopa.pnss.common.client.ConfigurationApiCall;
import it.pagopa.pnss.common.client.exception.RetentionException;
import it.pagopa.pnss.repositorymanager.entity.DocTypeEntity;
import it.pagopa.pnss.repositorymanager.entity.DocumentEntity;
import it.pagopa.pnss.testutils.annotation.SpringBootTestWebEnv;
import it.pagopa.pnss.transformation.service.S3Service;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.ObjectLockRetention;
import software.amazon.awssdk.services.s3.model.PutObjectRetentionResponse;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@SpringBootTestWebEnv
@AutoConfigureWebTestClient
class RetentionServiceTest {

    @Autowired
    private RetentionServiceImpl retentionService;

    @MockBean
    private ConfigurationApiCall configurationApiCall;

    @SpyBean
    S3Service s3Service;

    @ParameterizedTest
    @ValueSource(strings = {"10y", "5Y", "10d", "5D", "1Y 1d", "10y 5d", "1y 10D", "10Y 10D"})
    void getRetentionPeriodInDaysOk(String retentionPeriod) {
        Assertions.assertNotNull(retentionService.getRetentionPeriodInDays(retentionPeriod));
    }

    @ParameterizedTest
    @CsvSource({"documentKeyEnt, AVAILABLE, PN_NOTIFICATION_ATTACHMENTS, CLIENT_ID_123, apiKey_value"})
    void getRetentionPeriodInDaysMonoOk(String documentKey, String documentState, String documentType,
                                        String authPagopaSafestorageCxId, String authApiKey) {

        DocumentTypesConfigurations docTypeConfigurations = new DocumentTypesConfigurations();

        DocumentTypeConfiguration dtc = new DocumentTypeConfiguration();
        dtc.setName("PN_NOTIFICATION_ATTACHMENTS");

        DocumentTypeConfigurationStatuses documentTypeConfigurationStatuses = new DocumentTypeConfigurationStatuses();
        documentTypeConfigurationStatuses.setStorage("AVAILABLE");//TODO: guardare nel codice a cosa si riferisca il setStorage
        dtc.setStatuses(Map.of("AVAILABLE", documentTypeConfigurationStatuses));

        StorageConfiguration sc = new StorageConfiguration();
        sc.setName("AVAILABLE");
        sc.setRetentionPeriod("10y");

        docTypeConfigurations.setDocumentsTypes(List.of(dtc));
        docTypeConfigurations.setStorageConfigurations(List.of(sc));
        when(configurationApiCall.getDocumentsConfigs(anyString(), anyString())).thenReturn(Mono.just(docTypeConfigurations));

        var testMono = retentionService.getRetentionPeriodInDays(documentKey, documentState, documentType, authPagopaSafestorageCxId, authApiKey);

        StepVerifier.create(testMono).expectNextCount(1).verifyComplete();
    }

    @ParameterizedTest
    @CsvSource({"documentKeyEnt, AVAILABLE, PN_NOTIFICATION_ATTACHMENTS, CLIENT_ID_123, apiKey_value"})
    void getRetentionPeriodInDaysMonoKo(String documentKey, String documentState, String documentType,
                                        String authPagopaSafestorageCxId, String authApiKey) {

        DocumentTypesConfigurations docTypeConfigurations = new DocumentTypesConfigurations();

        DocumentTypeConfiguration dtc = new DocumentTypeConfiguration();
        dtc.setName("PN_NOTIFICATION_ATTACHMENTS");

        DocumentTypeConfigurationStatuses documentTypeConfigurationStatuses = new DocumentTypeConfigurationStatuses();
        dtc.setStatuses(Map.of("AVAILABLE", documentTypeConfigurationStatuses));

        StorageConfiguration sc = new StorageConfiguration();
        sc.setName("AVAILABLE");
        sc.setRetentionPeriod("10y");

        docTypeConfigurations.setDocumentsTypes(List.of(dtc));
        docTypeConfigurations.setStorageConfigurations(List.of(sc));
        when(configurationApiCall.getDocumentsConfigs(anyString(), anyString())).thenReturn(Mono.just(docTypeConfigurations));

        var testMono = retentionService.getRetentionPeriodInDays(documentKey, documentState, documentType, authPagopaSafestorageCxId, authApiKey);
        StepVerifier.create(testMono).expectError(RetentionException.class).verify();
    }

    @ParameterizedTest
    @CsvSource({"documentKeyEnt, , PN_NOTIFICATION_ATTACHMENTS, CLIENT_ID_123, apiKey_value"})
    void getRetentionPeriodInDaysMonoDocumentStateNull(String documentKey, String documentState, String documentType,
                                        String authPagopaSafestorageCxId, String authApiKey) throws RetentionException{

        Assertions.assertThrows(RetentionException.class, () -> retentionService.getRetentionPeriodInDays(documentKey, documentState, documentType, authPagopaSafestorageCxId, authApiKey));
    }

    @ParameterizedTest
    @CsvSource({"documentKeyEnt, AVAILABLE, , CLIENT_ID_123, apiKey_value"})
    void getRetentionPeriodInDaysMonoDocumentTypeNull(String documentKey, String documentState, String documentType,
                                        String authPagopaSafestorageCxId, String authApiKey) throws RetentionException{

        Assertions.assertThrows(RetentionException.class, () -> retentionService.getRetentionPeriodInDays(documentKey, documentState, documentType, authPagopaSafestorageCxId, authApiKey));
    }

    @ParameterizedTest
    @EmptySource
    @ValueSource(strings = {" ", "d"})
    void getRetentionPeriodInDaysKo(String retentionPeriod) {
        Assertions.assertThrows(RetentionException.class, () -> retentionService.getRetentionPeriodInDays(retentionPeriod));
    }

    @ParameterizedTest
    @CsvSource({"2023-08-18T14:38:40.108Z, 1"})
    void getRetainUntilDateOk(Instant dataCreazione, Integer retentionPeriod) throws RetentionException{

        Assertions.assertNotNull(retentionService.getRetainUntilDate(dataCreazione, retentionPeriod));
    }

    @Test
    void setRetentionPeriodInBucketObjectMetadataOk(){
        String authPagopaSafestorageCxId = "CLIENT_ID_123";
        String authApiKey = "apiKey_value";
        String oldState = "BOOKED";

        DocumentChanges documentChanges = new DocumentChanges();
        documentChanges.setRetentionUntil("2023-09-07T17:34:15+02:00");

        DocumentEntity documentEntity = new DocumentEntity();
        documentEntity.setDocumentKey("documentKeyEnt");

        PutObjectRetentionResponse putObjectRetentionResponse = PutObjectRetentionResponse.builder().build();

        when(s3Service.headObject(anyString(), anyString())).thenReturn(Mono.just(HeadObjectResponse.builder().objectLockRetainUntilDate(Instant.now()).build()));
        Mockito.doReturn(Mono.just(putObjectRetentionResponse)).when(s3Service).putObjectRetention(anyString(), anyString(), any(ObjectLockRetention.class));

        var testMono = retentionService.setRetentionPeriodInBucketObjectMetadata(authPagopaSafestorageCxId, authApiKey, documentChanges, documentEntity, oldState);

        StepVerifier.create(testMono).expectNextCount(1).verifyComplete();
    }

    @Test
    void setRetentionPeriodInBucketObjectMetadataRetentionWrong(){
        String authPagopaSafestorageCxId = "CLIENT_ID_123";
        String authApiKey = "apiKey_value";
        String oldState = "BOOKED";

        DocumentChanges documentChanges = new DocumentChanges();
        documentChanges.setRetentionUntil("2032-04-12T12:32:04.000Z");

        DocumentEntity documentEntity = new DocumentEntity();
        documentEntity.setDocumentKey("documentKeyEnt");

        PutObjectRetentionResponse putObjectRetentionResponse = PutObjectRetentionResponse.builder().build();

        when(s3Service.headObject(anyString(), anyString())).thenReturn(Mono.just(HeadObjectResponse.builder().objectLockRetainUntilDate(Instant.now()).build()));
        Mockito.doReturn(Mono.just(putObjectRetentionResponse)).when(s3Service).putObjectRetention(anyString(), anyString(), any(ObjectLockRetention.class));

        var testMono = retentionService.setRetentionPeriodInBucketObjectMetadata(authPagopaSafestorageCxId, authApiKey, documentChanges, documentEntity, oldState);

        StepVerifier.create(testMono).expectError(DateTimeParseException.class).verify();
    }

    @Test
    void setRetentionPeriodInBucketObjectMetadataRetentionUntilNullOk(){
        String authPagopaSafestorageCxId = "CLIENT_ID_123";
        String authApiKey = "apiKey_value";
        String oldState = "BOOKED";


        DocumentTypesConfigurations docTypeConfigurations = new DocumentTypesConfigurations();

        DocumentTypeConfiguration dtc = new DocumentTypeConfiguration();
        dtc.setName("PN_NOTIFICATION_ATTACHMENTS");

        DocumentTypeConfigurationStatuses documentTypeConfigurationStatuses = new DocumentTypeConfigurationStatuses();
        documentTypeConfigurationStatuses.setStorage("AVAILABLE");//TODO: guardare nel codice a cosa si riferisca il setStorage
        dtc.setStatuses(Map.of("AVAILABLE", documentTypeConfigurationStatuses));

        StorageConfiguration sc = new StorageConfiguration();
        sc.setName("AVAILABLE");
        sc.setRetentionPeriod("10y");

        docTypeConfigurations.setDocumentsTypes(List.of(dtc));
        docTypeConfigurations.setStorageConfigurations(List.of(sc));
        when(configurationApiCall.getDocumentsConfigs(anyString(), anyString())).thenReturn(Mono.just(docTypeConfigurations));


        DocumentChanges documentChanges = new DocumentChanges();
        documentChanges.setDocumentState("AVAILABLE");

        DocTypeEntity docTypeEntity = new DocTypeEntity();
        docTypeEntity.setTipoDocumento("PN_NOTIFICATION_ATTACHMENTS");

        DocumentEntity documentEntity = new DocumentEntity();
        documentEntity.setDocumentType(docTypeEntity);
        documentEntity.setDocumentLogicalState("AVAILABLE");
        documentEntity.setDocumentKey("documentKeyEnt");

        PutObjectRetentionResponse putObjectRetentionResponse = PutObjectRetentionResponse.builder().build();

//        HeadObjectResponse.builder().objectLockRetainUntilDate(Instant.now()).lastModified(Instant.now()).build();

        when(s3Service.headObject(anyString(), anyString())).thenReturn(Mono.just(HeadObjectResponse.builder().objectLockRetainUntilDate(Instant.now()).lastModified(Instant.now()).build()));
        Mockito.doReturn(Mono.just(putObjectRetentionResponse)).when(s3Service).putObjectRetention(anyString(), anyString(), any(ObjectLockRetention.class));

        var testMono = retentionService.setRetentionPeriodInBucketObjectMetadata(authPagopaSafestorageCxId, authApiKey, documentChanges, documentEntity, oldState);

        StepVerifier.create(testMono).expectNextCount(1).verifyComplete();
    }

    @Test
    void setRetentionPeriodInBucketObjectMetadataKO(){
        String authPagopaSafestorageCxId = "CLIENT_ID_123";
        String authApiKey = "apiKey_value";
        String oldState = "BOOKED";

        DocumentChanges documentChanges = new DocumentChanges();

        DocumentEntity documentEntity = new DocumentEntity();
        documentEntity.setDocumentKey("documentKeyEnt");

        var testMono = retentionService.setRetentionPeriodInBucketObjectMetadata(authPagopaSafestorageCxId, authApiKey, documentChanges, documentEntity, oldState);

        StepVerifier.create(testMono).expectError(NoSuchKeyException.class).verify();
    }
}
