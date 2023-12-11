package it.pagopa.pnss.transformation.service;

import io.awspring.cloud.messaging.listener.Acknowledgment;

import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.DocumentType;
import it.pagopa.pnss.common.client.exception.ArubaSignException;
import it.pagopa.pnss.common.client.exception.ArubaSignExceptionLimitCall;
import it.pagopa.pnss.common.exception.IllegalTransformationException;
import it.pagopa.pnss.common.exception.InvalidStatusTransformationException;
import it.pagopa.pnss.configurationproperties.BucketName;
import it.pagopa.pnss.configurationproperties.RepositoryManagerDynamoTableName;
import it.pagopa.pnss.repositorymanager.entity.CurrentStatusEntity;
import it.pagopa.pnss.repositorymanager.entity.DocTypeEntity;
import it.pagopa.pnss.repositorymanager.entity.DocumentEntity;
import it.pagopa.pnss.testutils.annotation.SpringBootTestWebEnv;
import it.pagopa.pnss.transformation.model.dto.BucketOriginDetail;
import it.pagopa.pnss.transformation.model.dto.CreatedS3ObjectDto;
import it.pagopa.pnss.transformation.model.dto.CreationDetail;
import it.pagopa.pnss.transformation.model.dto.S3Object;
import it.pagopa.pnss.transformation.rest.call.aruba.ArubaSignServiceCall;
import it.pagopa.pnss.transformation.wsdl.SignReturnV2;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

import static it.pagopa.pnss.common.DocTypesConstant.PN_AAR;
import static it.pagopa.pnss.common.constant.Constant.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@SpringBootTestWebEnv
public class TransformationServiceTest {

    @SpyBean
    private TransformationService transformationService;
    @MockBean
    private ArubaSignServiceCall arubaSignServiceCall;
    @Autowired
    private BucketName bucketName;
    @Autowired
    private S3Service s3Service;

    private static final String FILE_KEY = "FILE_KEY";

    private static DynamoDbTable<DocumentEntity> documentEntityDynamoDbTable;

    @BeforeAll
    public static void init(@Autowired DynamoDbEnhancedClient dynamoDbEnhancedClient, @Autowired RepositoryManagerDynamoTableName gestoreRepositoryDynamoDbTableName, @Value("${default.internal.x-api-key.value}") String defaultInternalApiKeyValue, @Value("${default.internal.header.x-pagopa-safestorage-cx-id}") String defaultInternalClientIdValue) {
       documentEntityDynamoDbTable = dynamoDbEnhancedClient.table(gestoreRepositoryDynamoDbTableName.documentiName(), TableSchema.fromBean(DocumentEntity.class));
  }
    @BeforeEach
    void initialize(@Autowired BucketName bucketName) {
        s3Service.putObject(FILE_KEY, new byte[10], bucketName.ssStageName()).block();
    }

    @Test
    void newStagingBucketObjectCreatedEventBlankDetail() {

        CreatedS3ObjectDto createdS3ObjectDto = new CreatedS3ObjectDto();

        Acknowledgment acknowledgment = new Acknowledgment() {
            @Override
            public Future<?> acknowledge() {
                return null;
            }
        };

        var testMono = transformationService.newStagingBucketObjectCreatedEvent(createdS3ObjectDto, acknowledgment);

        StepVerifier.create(testMono).expectNextCount(0).verifyComplete();
    }

    @Test
    void newStagingBucketObjectCreatedInvalidStatus() {

        S3Object s3Object = new S3Object();
        s3Object.setKey(FILE_KEY);

        BucketOriginDetail bucketOriginDetail = new BucketOriginDetail();
        bucketOriginDetail.setName(bucketName.ssStageName());

        CreationDetail creationDetail = new CreationDetail();
        creationDetail.setObject(s3Object);
        creationDetail.setBucketOriginDetail(bucketOriginDetail);

        CreatedS3ObjectDto createdS3ObjectDto = new CreatedS3ObjectDto();
        createdS3ObjectDto.setCreationDetailObject(creationDetail);

        Acknowledgment acknowledgment = new Acknowledgment() {
            @Override
            public Future<?> acknowledge() {
                return null;
            }
        };

        insertDocumentEntity(FILE_KEY, "application/pdf", AVAILABLE, List.of(DocumentType.TransformationsEnum.SIGN_AND_TIMEMARK));

        var testMono = transformationService.newStagingBucketObjectCreatedEvent(createdS3ObjectDto, acknowledgment);

        StepVerifier.create(testMono).expectError(InvalidStatusTransformationException.class).verify();
        verify(transformationService, times(1)).objectTransformation(anyString(), anyString(), anyBoolean());
        deleteDocumentEntity(FILE_KEY);
    }

    @Test
    void newStagingBucketObjectCreatedInvalidTransformation() {

        S3Object s3Object = new S3Object();
        s3Object.setKey("111-DDD");

        BucketOriginDetail bucketOriginDetail = new BucketOriginDetail();
        bucketOriginDetail.setName("prova");

        CreationDetail creationDetail = new CreationDetail();
        creationDetail.setObject(s3Object);
        creationDetail.setBucketOriginDetail(bucketOriginDetail);

        CreatedS3ObjectDto createdS3ObjectDto = new CreatedS3ObjectDto();
        createdS3ObjectDto.setCreationDetailObject(creationDetail);

        Acknowledgment acknowledgment = new Acknowledgment() {
            @Override
            public Future<?> acknowledge() {
                return null;
            }
        };

        insertDocumentEntity("111-DDD","application/pdf", STAGED, List.of(DocumentType.TransformationsEnum.NONE));

        var testMono = transformationService.newStagingBucketObjectCreatedEvent(createdS3ObjectDto, acknowledgment);

        StepVerifier.create(testMono).expectError(IllegalTransformationException.class).verify();
        verify(transformationService, times(1)).objectTransformation(anyString(), anyString(), anyBoolean());
        deleteDocumentEntity("111-DDD");
    }

    @Test
    void newStagingBucketObjectCreatedEventArubaKo() {

        S3Object s3Object = new S3Object();
        s3Object.setKey(FILE_KEY);

        BucketOriginDetail bucketOriginDetail = new BucketOriginDetail();
        bucketOriginDetail.setName(bucketName.ssStageName());

        CreationDetail creationDetail = new CreationDetail();
        creationDetail.setObject(s3Object);
        creationDetail.setBucketOriginDetail(bucketOriginDetail);

        CreatedS3ObjectDto createdS3ObjectDto = new CreatedS3ObjectDto();
        createdS3ObjectDto.setCreationDetailObject(creationDetail);

        Acknowledgment acknowledgment = new Acknowledgment() {
            @Override
            public Future<?> acknowledge() {
                return null;
            }
        };

        insertDocumentEntity(FILE_KEY, "application/pdf", STAGED, List.of(DocumentType.TransformationsEnum.SIGN_AND_TIMEMARK));
        when(arubaSignServiceCall.signPdfDocument(any(), anyBoolean())).thenReturn(Mono.error(new ArubaSignException()));

        var testMono = transformationService.newStagingBucketObjectCreatedEvent(createdS3ObjectDto, acknowledgment);

        StepVerifier.create(testMono).expectError(ArubaSignExceptionLimitCall.class).verify();
        deleteDocumentEntity(FILE_KEY);
    }

    @ParameterizedTest
    @ValueSource(strings = {"application/pdf", "application/xml", "other"})
    void newStagingBucketObjectCreatedEventOk(String contentType) {

        S3Object s3Object = new S3Object();
        s3Object.setKey(FILE_KEY);

        BucketOriginDetail bucketOriginDetail = new BucketOriginDetail();
        bucketOriginDetail.setName(bucketName.ssStageName());

        CreationDetail creationDetail = new CreationDetail();
        creationDetail.setObject(s3Object);
        creationDetail.setBucketOriginDetail(bucketOriginDetail);

        CreatedS3ObjectDto createdS3ObjectDto = new CreatedS3ObjectDto();
        createdS3ObjectDto.setCreationDetailObject(creationDetail);

        Acknowledgment acknowledgment = new Acknowledgment() {
            @Override
            public Future<?> acknowledge() {
                return null;
            }
        };

        insertDocumentEntity(FILE_KEY, contentType, STAGED, List.of(DocumentType.TransformationsEnum.SIGN_AND_TIMEMARK));
        mockArubaCalls();

        var testMono = transformationService.newStagingBucketObjectCreatedEvent(createdS3ObjectDto, acknowledgment);

        StepVerifier.create(testMono).expectNextCount(0).verifyComplete();
        verify(transformationService, times(1)).objectTransformation(anyString(), anyString(), anyBoolean());
        deleteDocumentEntity(FILE_KEY);
    }

    void insertDocumentEntity(String fileKey, String contentType, String documentState, List<DocumentType.TransformationsEnum> transformations) {
        DocTypeEntity docTypeEntity = new DocTypeEntity();
        docTypeEntity.setStatuses(Map.ofEntries(Map.entry(PRELOADED, new CurrentStatusEntity())));
        docTypeEntity.setTipoDocumento(PN_AAR);
        docTypeEntity.setTransformations(transformations);
        DocumentEntity documentEntity = new DocumentEntity();
        documentEntity.setDocumentKey(fileKey);
        documentEntity.setDocumentState(documentState);
        documentEntity.setContentType(contentType);
        documentEntity.setDocumentType(docTypeEntity);
        documentEntityDynamoDbTable.putItem(documentEntity);
    }

    void deleteDocumentEntity(String fileKey) {
        DocumentEntity documentEntity = new DocumentEntity();
        documentEntity.setDocumentKey(fileKey);
        documentEntityDynamoDbTable.deleteItem(documentEntity);
    }

    void mockArubaCalls() {
        SignReturnV2 signReturnV2 = new SignReturnV2();
        signReturnV2.setDescription("test");
        signReturnV2.setReturnCode("ok");
        signReturnV2.setStatus("ok");
        signReturnV2.setBinaryoutput(new byte[10]);

        when(arubaSignServiceCall.signPdfDocument(any(), anyBoolean())).thenReturn(Mono.just(signReturnV2));
        when(arubaSignServiceCall.xmlSignature(any(), anyBoolean())).thenReturn(Mono.just(signReturnV2));
        when(arubaSignServiceCall.pkcs7signV2(any(), anyBoolean())).thenReturn(Mono.just(signReturnV2));
    }

}
