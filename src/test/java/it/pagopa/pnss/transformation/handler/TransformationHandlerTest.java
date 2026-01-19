package it.pagopa.pnss.transformation.handler;

import io.awspring.cloud.messaging.listener.Acknowledgment;
import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.TransformationMessage;
import it.pagopa.pnss.testutils.annotation.SpringBootTestWebEnv;
import it.pagopa.pnss.transformation.model.dto.S3BucketOriginDetail;
import it.pagopa.pnss.transformation.model.dto.S3EventNotificationDetail;
import it.pagopa.pnss.transformation.model.dto.S3EventNotificationMessage;
import it.pagopa.pnss.transformation.model.dto.S3Object;
import it.pagopa.pnss.transformation.service.S3Service;
import it.pagopa.pnss.transformation.service.TransformationService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import reactor.core.publisher.Mono;
import reactor.test.publisher.TestPublisher;
import software.amazon.awssdk.services.s3.model.*;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTestWebEnv
class TransformationHandlerTest {

    @MockBean
    private TransformationService transformationService;
    @SpyBean
    private S3Service s3Service;
    @Autowired
    private TransformationHandler transformationHandler;
    private static final S3EventNotificationMessage S3_EVENT_NOTIFICATION = createS3EventMessage();
    private final TransformationMessage transformationMessage = createTransformationMessage();

    @Test
    void processAndPublishTransformation_Ok() {
        //GIVEN
        TestPublisher<Void> testPublisher = TestPublisher.createCold();
        testPublisher.complete();
        Acknowledgment acknowledgment = mock(Acknowledgment.class);
        String fileKey = S3_EVENT_NOTIFICATION.getEventNotificationDetail().getObject().getKey();
        //WHEN
        List<ObjectVersion> versions = List.of(
                ObjectVersion.builder()
                        .key(fileKey)
                        .versionId("v1")
                        .isLatest(false)
                        .build());
        ListObjectVersionsResponse listObjectVersionsResponse = ListObjectVersionsResponse.builder().versions(versions).build();
        when(s3Service.listObjectVersions(any(String.class), any(String.class))).thenReturn(Mono.just(listObjectVersionsResponse));
        when(transformationService.handleS3Event(any(S3EventNotificationMessage.class))).thenReturn(testPublisher.mono());

        //THEN
        Assertions.assertDoesNotThrow(() -> transformationHandler.processAndPublishTransformation(S3_EVENT_NOTIFICATION, acknowledgment));
        testPublisher.assertWasSubscribed();
        verify(acknowledgment).acknowledge();
    }

    @Test
    void processAndPublishTransformation_Ko() {
        //GIVEN
        TestPublisher<Void> testPublisher = TestPublisher.createCold();
        testPublisher.error(NoSuchKeyException.builder().build());
        Acknowledgment acknowledgment = mock(Acknowledgment.class);
        String fileKey = S3_EVENT_NOTIFICATION.getEventNotificationDetail().getObject().getKey();
        //WHEN
        List<ObjectVersion> versions = List.of(
                ObjectVersion.builder()
                        .key(fileKey)
                        .versionId("v1")
                        .isLatest(false)
                        .build());
        ListObjectVersionsResponse listObjectVersionsResponse = ListObjectVersionsResponse.builder().versions(versions).build();
        when(s3Service.listObjectVersions(any(String.class), any(String.class))).thenReturn(Mono.just(listObjectVersionsResponse));
        when(transformationService.handleS3Event(any(S3EventNotificationMessage.class))).thenReturn(testPublisher.mono());

        //THEN
        Assertions.assertDoesNotThrow(() -> transformationHandler.processAndPublishTransformation(S3_EVENT_NOTIFICATION, acknowledgment));
        testPublisher.assertWasSubscribed();
        verify(acknowledgment, never()).acknowledge();
    }

    @Test
    void processAndPublishTransformation_MultipleVersion_Ok() {
        //GIVEN
        TestPublisher<Void> testPublisher = TestPublisher.createCold();
        testPublisher.complete();
        Acknowledgment acknowledgment = mock(Acknowledgment.class);
        String fileKey = S3_EVENT_NOTIFICATION.getEventNotificationDetail().getObject().getKey();

        //WHEN
        S3BucketOriginDetail s3BucketOriginDetail = S3BucketOriginDetail.builder().name("source-bucket").build();
        S3_EVENT_NOTIFICATION.getEventNotificationDetail().setS3BucketOriginDetail(s3BucketOriginDetail);
        S3_EVENT_NOTIFICATION.getEventNotificationDetail().setReason("PutObject");
        List<ObjectVersion> versions = List.of(
                ObjectVersion.builder()
                        .key(fileKey)
                        .versionId("v1")
                        .isLatest(false)
                        .build(),
                ObjectVersion.builder()
                        .key(fileKey)
                        .versionId("v2")
                        .isLatest(true)
                        .build()
        );
        ListObjectVersionsResponse listObjectVersionsResponse = ListObjectVersionsResponse.builder().versions(versions).build();
        when(s3Service.listObjectVersions(fileKey, s3BucketOriginDetail.getName())).thenReturn(Mono.just(listObjectVersionsResponse));
        //THEN
        Assertions.assertDoesNotThrow(() -> transformationHandler.processAndPublishTransformation(S3_EVENT_NOTIFICATION, acknowledgment));
        //skip handleS3
        verify(transformationService, never()).handleS3Event(any(S3EventNotificationMessage.class));
        verify(acknowledgment).acknowledge();
    }


    @Test
    void signAndTimemarkTransformationSubscriber_Ok() {
        //GIVEN
        TestPublisher<PutObjectResponse> testPublisher = TestPublisher.createCold();
        testPublisher.next(PutObjectResponse.builder().build());
        Acknowledgment acknowledgment = mock(Acknowledgment.class);

        //WHEN
        when(transformationService.signAndTimemarkTransformation(any(TransformationMessage.class), eq(true),anyString())).thenReturn(testPublisher.mono());

        //THEN
        Assertions.assertDoesNotThrow(() -> transformationHandler.signAndTimemarkTransformationSubscriber(transformationMessage, acknowledgment));
        testPublisher.assertWasSubscribed();
        verify(acknowledgment).acknowledge();
    }

    @Test
    void signAndTimemarkTransformationSubscriber_Ko() {
        //GIVEN
        TestPublisher<PutObjectResponse> testPublisher = TestPublisher.createCold();
        testPublisher.error(NoSuchKeyException.builder().build());
        Acknowledgment acknowledgment = mock(Acknowledgment.class);

        //WHEN
        when(transformationService.signAndTimemarkTransformation(any(TransformationMessage.class), eq(true),anyString())).thenReturn(testPublisher.mono());

        //THEN
        Assertions.assertDoesNotThrow(() -> transformationHandler.signAndTimemarkTransformationSubscriber(transformationMessage, acknowledgment));
        testPublisher.assertWasSubscribed();
        verify(acknowledgment, never()).acknowledge();
    }

    @Test
    void signTransformationSubscriber_Ok() {
        //GIVEN
        TestPublisher<PutObjectResponse> testPublisher = TestPublisher.createCold();
        testPublisher.next(PutObjectResponse.builder().build());
        Acknowledgment acknowledgment = mock(Acknowledgment.class);

        //WHEN
        when(transformationService.signAndTimemarkTransformation(any(TransformationMessage.class), eq(false),anyString())).thenReturn(testPublisher.mono());

        //THEN
        Assertions.assertDoesNotThrow(() -> transformationHandler.signTransformationSubscriber(createTransformationMessage(), acknowledgment));
        testPublisher.assertWasSubscribed();
        verify(acknowledgment).acknowledge();
    }

    @Test
    void signTransformationSubscriber_Ko() {
        //GIVEN
        TestPublisher<PutObjectResponse> testPublisher = TestPublisher.createCold();
        testPublisher.error(NoSuchKeyException.builder().build());
        Acknowledgment acknowledgment = mock(Acknowledgment.class);

        //WHEN
        when(transformationService.signAndTimemarkTransformation(any(TransformationMessage.class), eq(false),anyString())).thenReturn(testPublisher.mono());

        //THEN
        Assertions.assertDoesNotThrow(() -> transformationHandler.signTransformationSubscriber(transformationMessage, acknowledgment));
        testPublisher.assertWasSubscribed();
        verify(acknowledgment, never()).acknowledge();
    }

    @Test
    void dummyTransformationSubscriber_Ok() {
        //GIVEN
        TestPublisher<PutObjectTaggingResponse> testPublisher = TestPublisher.createCold();
        testPublisher.next(PutObjectTaggingResponse.builder().build());
        Acknowledgment acknowledgment = mock(Acknowledgment.class);

        //WHEN
        when(transformationService.dummyTransformation(any(TransformationMessage.class))).thenReturn(testPublisher.mono());

        //THEN
        Assertions.assertDoesNotThrow(() -> transformationHandler.dummyTransformationSubscriber(transformationMessage, acknowledgment));
        testPublisher.assertWasSubscribed();
        verify(acknowledgment).acknowledge();
    }

    @Test
    void dummyTransformationSubscriber_Ko() {
        //GIVEN
        TestPublisher<PutObjectTaggingResponse> testPublisher = TestPublisher.createCold();
        testPublisher.error(NoSuchKeyException.builder().build());
        Acknowledgment acknowledgment = mock(Acknowledgment.class);

        //WHEN
        when(transformationService.dummyTransformation(any(TransformationMessage.class))).thenReturn(testPublisher.mono());

        //THEN
        Assertions.assertDoesNotThrow(() -> transformationHandler.dummyTransformationSubscriber(transformationMessage, acknowledgment));
        testPublisher.assertWasSubscribed();
        verify(acknowledgment, never()).acknowledge();
    }

    private TransformationMessage createTransformationMessage() {
        TransformationMessage transformationMessage = new TransformationMessage();
        String fileKey = "fileKey";
        transformationMessage.setFileKey(fileKey);
        return transformationMessage;
    }

    private static S3EventNotificationMessage createS3EventMessage() {
        S3Object s3Object = S3Object.builder().key("fileKey").build();
        S3EventNotificationDetail detail = S3EventNotificationDetail.builder().object(s3Object).build();
        return S3EventNotificationMessage.builder().eventNotificationDetail(detail).build();
    }

}
