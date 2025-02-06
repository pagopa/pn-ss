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
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectTaggingResponse;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTestWebEnv
class TransformationHandlerTest {

    @MockBean
    private TransformationService transformationService;
    @Autowired
    private TransformationHandler transformationHandler;
    private final TransformationMessage TRANSFORMATION_MESSAGE = createTransformationMessage();

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
