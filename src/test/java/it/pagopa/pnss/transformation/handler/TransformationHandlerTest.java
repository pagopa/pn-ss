package it.pagopa.pnss.transformation.handler;

import io.awspring.cloud.messaging.listener.Acknowledgment;
import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.TransformationMessage;
import it.pagopa.pnss.testutils.annotation.SpringBootTestWebEnv;
import it.pagopa.pnss.transformation.service.TransformationService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import reactor.test.publisher.TestPublisher;
import software.amazon.awssdk.eventnotifications.s3.model.S3EventNotification;
import software.amazon.awssdk.eventnotifications.s3.model.S3EventNotificationRecord;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectTaggingResponse;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTestWebEnv
class TransformationHandlerTest {

    @MockBean
    private TransformationService transformationService;
    @Autowired
    private TransformationHandler transformationHandler;
    private static final S3EventNotification S3_EVENT_NOTIFICATION = new S3EventNotification(List.of(new S3EventNotificationRecord()));
    private final TransformationMessage TRANSFORMATION_MESSAGE = createTransformationMessage();

    @Test
    void processAndPublishTransformation_Ok() {
        //GIVEN
        TestPublisher<Void> testPublisher = TestPublisher.createCold();
        testPublisher.complete();
        Acknowledgment acknowledgment = mock(Acknowledgment.class);

        //WHEN
        when(transformationService.handleS3Event(any(S3EventNotificationRecord.class))).thenReturn(testPublisher.mono());

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

        //WHEN
        when(transformationService.handleS3Event(any(S3EventNotificationRecord.class))).thenReturn(testPublisher.mono());

        //THEN
        Assertions.assertDoesNotThrow(() -> transformationHandler.processAndPublishTransformation(S3_EVENT_NOTIFICATION, acknowledgment));
        testPublisher.assertWasSubscribed();
        verify(acknowledgment, never()).acknowledge();
    }


    @Test
    void signAndTimemarkTransformationSubscriber_Ok() {
        //GIVEN
        TestPublisher<PutObjectResponse> testPublisher = TestPublisher.createCold();
        testPublisher.next(PutObjectResponse.builder().build());
        Acknowledgment acknowledgment = mock(Acknowledgment.class);

        //WHEN
        when(transformationService.signAndTimemarkTransformation(any(TransformationMessage.class), eq(true))).thenReturn(testPublisher.mono());

        //THEN
        Assertions.assertDoesNotThrow(() -> transformationHandler.signAndTimemarkTransformationSubscriber(TRANSFORMATION_MESSAGE, acknowledgment));
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
        when(transformationService.signAndTimemarkTransformation(any(TransformationMessage.class), eq(true))).thenReturn(testPublisher.mono());

        //THEN
        Assertions.assertDoesNotThrow(() -> transformationHandler.signAndTimemarkTransformationSubscriber(TRANSFORMATION_MESSAGE, acknowledgment));
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
        when(transformationService.signAndTimemarkTransformation(any(TransformationMessage.class), eq(false))).thenReturn(testPublisher.mono());

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
        when(transformationService.signAndTimemarkTransformation(any(TransformationMessage.class), eq(false))).thenReturn(testPublisher.mono());

        //THEN
        Assertions.assertDoesNotThrow(() -> transformationHandler.signTransformationSubscriber(TRANSFORMATION_MESSAGE, acknowledgment));
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
        Assertions.assertDoesNotThrow(() -> transformationHandler.dummyTransformationSubscriber(TRANSFORMATION_MESSAGE, acknowledgment));
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
        Assertions.assertDoesNotThrow(() -> transformationHandler.dummyTransformationSubscriber(TRANSFORMATION_MESSAGE, acknowledgment));
        testPublisher.assertWasSubscribed();
        verify(acknowledgment, never()).acknowledge();
    }

    private TransformationMessage createTransformationMessage() {
        TransformationMessage transformationMessage = new TransformationMessage();
        String FILE_KEY = "fileKey";
        transformationMessage.setFileKey(FILE_KEY);
        return transformationMessage;
    }

}
