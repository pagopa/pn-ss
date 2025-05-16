package it.pagopa.pnss.transformation.service;

import it.pagopa.pn.library.sign.pojo.PnSignDocumentResponse;
import it.pagopa.pn.library.sign.service.impl.PnSignProviderService;
import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.*;
import it.pagopa.pnss.common.DocTypesConstant;
import it.pagopa.pnss.common.client.DocumentClientCall;
import it.pagopa.pnss.configuration.sqs.SqsTimeoutProvider;
import it.pagopa.pnss.configurationproperties.AvailabelDocumentEventBridgeName;
import it.pagopa.pnss.configurationproperties.BucketName;
import it.pagopa.pnss.testutils.annotation.SpringBootTestWebEnv;
import it.pagopa.pnss.transformation.model.dto.S3EventNotificationDetail;
import it.pagopa.pnss.transformation.model.dto.S3EventNotificationMessage;
import it.pagopa.pnss.transformation.model.dto.S3Object;
import lombok.CustomLog;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.provider.Arguments;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import java.util.stream.Stream;

import static it.pagopa.pnss.common.constant.Constant.PRELOADED;
import static it.pagopa.pnss.common.constant.Constant.STAGED;
import static it.pagopa.pnss.configurationproperties.TransformationProperties.OK;
import static it.pagopa.pnss.configurationproperties.TransformationProperties.TRANSFORMATION_TAG_PREFIX;
import static it.pagopa.pnss.transformation.utils.TransformationUtils.OBJECT_CREATED_PUT_EVENT;
import static it.pagopa.pnss.transformation.utils.TransformationUtils.PUT_OBJECT_REASON;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTestWebEnv
@CustomLog
class TransformationServiceTimeoutTest {
    @SpyBean
    private TransformationService transformationService;
    @MockBean
    private DocumentClientCall documentClientCall;
    @Autowired
    private BucketName bucketName;
    @Autowired
    private S3Client s3TestClient;
    @Autowired
    AvailabelDocumentEventBridgeName availabelDocumentEventBridgeName;
    @SpyBean
    private S3Service s3Service;
    @SpyBean
    private PnSignProviderService pnSignProviderService;
    @SpyBean
    private SqsTimeoutProvider sqsTimeoutProvider;
    private final String FILE_KEY = "FILE_KEY";
    private static final String SIGN_AND_TIMEMARK = "SIGN_AND_TIMEMARK";
    private static final String SIGN = "SIGN";
    private static final String QUEUE_NAME="queue";
    private static final Duration TIMEOUT_INACTIVE_DURATION = Duration.ofSeconds(86400);


    @BeforeEach
    void initialize() {
        putObjectInBucket(FILE_KEY, bucketName.ssStageName(), new byte[10]);
    }

    @AfterEach
    void clean() {
        deleteObjectInBucket(FILE_KEY, bucketName.ssHotName());
        deleteObjectInBucket(FILE_KEY, bucketName.ssStageName());
    }

    @Test
    void handleS3Event_FirstTransformation_ShortTimeout() {

        when(sqsTimeoutProvider.getTimeoutForQueue(anyString())).thenReturn(Duration.ofMillis(2));
        //GIVEN
        String sourceBucket = bucketName.ssStageName();
        String contentType = "application/pdf";
        String nextTransformation = SIGN_AND_TIMEMARK;
        S3EventNotificationMessage record = createS3Event(OBJECT_CREATED_PUT_EVENT);
        TransformationMessage expectedMessage = createTransformationMessage(nextTransformation, sourceBucket, contentType);

        //WHEN
        mockGetDocument(contentType, STAGED, List.of(nextTransformation));
        var testMono = transformationService.handleS3Event(record,QUEUE_NAME);

        //THEN
        StepVerifier.create(testMono)
                .expectErrorMatches(throwable -> throwable instanceof TimeoutException)
                .verify();
    }

    @Test
    void handleS3Event_LongTimeout() {

        when(sqsTimeoutProvider.getTimeoutForQueue(anyString())).thenReturn(Duration.ofSeconds(10));
        //GIVEN
        String sourceBucket = bucketName.ssStageName();
        String contentType = "application/pdf";
        String nextTransformation = SIGN_AND_TIMEMARK;
        S3EventNotificationMessage record = createS3Event(OBJECT_CREATED_PUT_EVENT);
        TransformationMessage expectedMessage = createTransformationMessage(nextTransformation, sourceBucket, contentType);

        //WHEN
        mockGetDocument(contentType, STAGED, List.of(nextTransformation));
        var testMono = transformationService.handleS3Event(record,QUEUE_NAME);

        //THEN
        StepVerifier.create(testMono)
                .expectComplete()
                .verify();
    }

    @Test
    void handleS3Event_NoTimeout() {

        when(sqsTimeoutProvider.getTimeoutForQueue(anyString())).thenReturn(TIMEOUT_INACTIVE_DURATION);
        //GIVEN
        String sourceBucket = bucketName.ssStageName();
        String contentType = "application/pdf";
        String nextTransformation = SIGN_AND_TIMEMARK;
        S3EventNotificationMessage record = createS3Event(OBJECT_CREATED_PUT_EVENT);
        TransformationMessage expectedMessage = createTransformationMessage(nextTransformation, sourceBucket, contentType);

        //WHEN
        mockGetDocument(contentType, STAGED, List.of(nextTransformation));
        var testMono = transformationService.handleS3Event(record,QUEUE_NAME);

        //THEN
        StepVerifier.create(testMono)
                .expectComplete()
                .verify();
    }

    @Test
    void signAndTimemark_LongTimeout() {
        //GIVEN
        String contentType="application/pdf";
        String bucket = bucketName.ssStageName();
        Tag tag = Tag.builder().key(TRANSFORMATION_TAG_PREFIX + SIGN_AND_TIMEMARK).value(OK).build();
        Tagging expectedTagging = Tagging.builder().tagSet(tag).build();

        //WHEN
        when(sqsTimeoutProvider.getTimeoutForQueue(anyString())).thenReturn(Duration.ofSeconds(10));

        mockSignCalls();
        var testMono = transformationService.signAndTimemarkTransformation(createTransformationMessage(SIGN_AND_TIMEMARK, bucket, contentType), true,QUEUE_NAME);

        //THEN
        StepVerifier.create(testMono).expectNextMatches(PutObjectResponse.class::isInstance).verifyComplete();
        verify(s3Service).getObject(FILE_KEY, bucket);
        verifyPnSignProviderCalls(contentType, true);
        verify(s3Service).putObject(eq(FILE_KEY), any(), eq(contentType), eq(bucket), eq(expectedTagging));
    }

    @Test
    void signAndTimemark_NoTimeout() {
        //GIVEN
        String contentType="application/pdf";
        String bucket = bucketName.ssStageName();
        Tag tag = Tag.builder().key(TRANSFORMATION_TAG_PREFIX + SIGN_AND_TIMEMARK).value(OK).build();
        Tagging expectedTagging = Tagging.builder().tagSet(tag).build();

        //WHEN
        when(sqsTimeoutProvider.getTimeoutForQueue(anyString())).thenReturn(TIMEOUT_INACTIVE_DURATION);

        mockSignCalls();
        var testMono = transformationService.signAndTimemarkTransformation(createTransformationMessage(SIGN_AND_TIMEMARK, bucket, contentType), true,QUEUE_NAME);

        //THEN
        StepVerifier.create(testMono).expectNextMatches(PutObjectResponse.class::isInstance).verifyComplete();
        verify(s3Service).getObject(FILE_KEY, bucket);
        verifyPnSignProviderCalls(contentType, true);
        verify(s3Service).putObject(eq(FILE_KEY), any(), eq(contentType), eq(bucket), eq(expectedTagging));
    }

    @Test
    void signAndTimemark_ShortTimeout() {
        //GIVEN
        String contentType="application/pdf";
        String bucket = bucketName.ssStageName();
        Tag tag = Tag.builder().key(TRANSFORMATION_TAG_PREFIX + SIGN_AND_TIMEMARK).value(OK).build();
        Tagging expectedTagging = Tagging.builder().tagSet(tag).build();

        //WHEN
        when(sqsTimeoutProvider.getTimeoutForQueue(anyString())).thenReturn(Duration.ofMillis(2));

        mockSignCalls();
        var testMono = transformationService.signAndTimemarkTransformation(createTransformationMessage(SIGN_AND_TIMEMARK, bucket, contentType), true,QUEUE_NAME);

        //THEN
        StepVerifier.create(testMono).expectErrorMatches(throwable -> throwable instanceof  TimeoutException).verify();
    }




    private TransformationMessage createTransformationMessage(String transformationType, String bucketName, String contentType, String fileKey) {
        it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.TransformationMessage transformationMessage = new it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.TransformationMessage();
        transformationMessage.setFileKey(fileKey);
        transformationMessage.setTransformationType(transformationType);
        transformationMessage.setBucketName(bucketName);
        transformationMessage.setContentType(contentType);
        return transformationMessage;
    }

    private TransformationMessage createTransformationMessage(String transformationType, String bucketName, String contentType) {
        return createTransformationMessage(transformationType, bucketName, contentType, FILE_KEY);
    }

    void mockGetDocument(String contentType, String documentState, List<String> transformations) {
        var documentType1 = new DocumentType().statuses(Map.ofEntries(Map.entry(PRELOADED, new CurrentStatus())))
                .tipoDocumento(DocTypesConstant.PN_AAR)
                .checksum(DocumentType.ChecksumEnum.MD5);
        var document = new Document().documentType(documentType1);
        document.setDocumentKey(FILE_KEY);
        document.setContentType(contentType);
        document.getDocumentType().setTransformations(transformations);
        document.setDocumentState(documentState);
        var documentResponse = new DocumentResponse().document(document);

        when(documentClientCall.getDocument(anyString())).thenReturn(Mono.just(documentResponse));
        when(documentClientCall.patchDocument(anyString(), anyString(), anyString(), any(DocumentChanges.class))).thenReturn(Mono.just(documentResponse));
    }

    void mockSignCalls() {
        PnSignDocumentResponse pnSignDocumentResponse = new PnSignDocumentResponse();
        pnSignDocumentResponse.setSignedDocument(new byte[10]);
        when(pnSignProviderService.signPdfDocument(any(), anyBoolean())).thenReturn(Mono.just(pnSignDocumentResponse));
        when(pnSignProviderService.signXmlDocument(any(), anyBoolean())).thenReturn(Mono.just(pnSignDocumentResponse));
        when(pnSignProviderService.pkcs7Signature(any(), anyBoolean())).thenReturn(Mono.just(pnSignDocumentResponse));
    }

    void verifyPnSignProviderCalls(String contentType, boolean marcatura) {
        switch (contentType) {
            case "application/pdf" -> verify(pnSignProviderService, times(1)).signPdfDocument(any(), eq(marcatura));
            case "application/xml" -> verify(pnSignProviderService, times(1)).signXmlDocument(any(), eq(marcatura));
            case "other" -> verify(pnSignProviderService, times(1)).pkcs7Signature(any(), eq(marcatura));
        }
    }

    private void putObjectInBucket(String key, String bucketName, byte[] fileBytes) {
        s3TestClient.putObject(PutObjectRequest.builder()
                        .key(key)
                        .bucket(bucketName)
                        .contentMD5(new String(Base64.encodeBase64(DigestUtils.md5(fileBytes))))
                        .expires(Instant.now().plus(Duration.ofDays(1))).build(),
                RequestBody.fromBytes(fileBytes));
    }

    private void deleteObjectInBucket(String key, String bucketName) {
        ListObjectVersionsResponse response = s3TestClient.listObjectVersions(ListObjectVersionsRequest.builder().prefix(key).bucket(bucketName).build());
        response.versions().forEach(version -> s3TestClient.deleteObject(DeleteObjectRequest.builder().bucket(bucketName).key(version.key()).versionId(version.versionId()).build()));
        response.deleteMarkers().forEach(deleteMarker -> s3TestClient.deleteObject(DeleteObjectRequest.builder().bucket(bucketName).key(deleteMarker.key()).versionId(deleteMarker.versionId()).build()));
    }

    private S3EventNotificationMessage createS3Event(String eventName, String fileKey) {
        S3Object s3Object = S3Object.builder().key(fileKey).build();
        S3EventNotificationDetail detail = S3EventNotificationDetail.builder()
                .object(s3Object)
                .reason(eventName.equals(OBJECT_CREATED_PUT_EVENT) ? PUT_OBJECT_REASON : null)
                .build();
        return S3EventNotificationMessage.builder().detailType(eventName).eventNotificationDetail(detail).build();
    }

    private S3EventNotificationMessage createS3Event(String eventName) {
        return createS3Event(eventName, FILE_KEY);
    }

    private static Stream<Arguments> provideSignAndTimemarkArgs() {
        return Stream.of(Arguments.of(SIGN_AND_TIMEMARK, true), Arguments.of(SIGN, false));
    }


}