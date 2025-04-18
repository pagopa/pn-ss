package it.pagopa.pnss.common.service;


import it.pagopa.pnss.testutils.annotation.SpringBootTestWebEnv;
import it.pagopa.pnss.transformation.model.dto.S3EventNotificationMessage;
import lombok.CustomLog;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageResponse;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;

@SpringBootTestWebEnv
@AutoConfigureWebTestClient
@CustomLog
class SqsServiceTest {
    @Autowired
    SqsService sqsService;
    @Value("${pn.ss.transformation.queues.staging}")
    private String signQueueName;
    @Autowired
    private SqsAsyncClient sqsAsyncClient;
    private static final Integer MAX_MESSAGES = 10;

    @Test
    void testSendOk() {
        S3EventNotificationMessage createdS3ObjectDto = new S3EventNotificationMessage();
        log.info(signQueueName);

        Mono<SendMessageResponse> sendMessageResponseMono = sqsService.send(signQueueName, createdS3ObjectDto);
        StepVerifier.create(sendMessageResponseMono)
               .expectNextCount(1)
                .verifyComplete();


    }

    @Test
    void testSendWrongQueueName() {
        S3EventNotificationMessage createdS3ObjectDto = new S3EventNotificationMessage();
        log.info(signQueueName);
        String queueName = "signQueueName";
        StepVerifier.create(sqsService.send(queueName, createdS3ObjectDto))
                .expectError()
                .verify();
    }

    @Test
    void getMessages() {
        sendMessageToQueue().block();

        StepVerifier.create(sqsService.getMessages(signQueueName, S3EventNotificationMessage.class, MAX_MESSAGES))
                .expectNextCount(1)
                .verifyComplete();
    }

    @Test
    void deleteMessageFromQueue() {
        SendMessageResponse sendMessageResponse = sendMessageToQueue().block();
        assert sendMessageResponse != null : "sendMessageResponse is null";
        Message received = sqsService.getMessages(signQueueName, S3EventNotificationMessage.class, MAX_MESSAGES).filter(sqsMessageWrapper -> sqsMessageWrapper.getMessage().messageId().equals(sendMessageResponse.messageId())).blockFirst().getMessage();
        assert received != null : "received is null";

        Mono<DeleteMessageResponse> deleteMessageResponseMono = sqsService.deleteMessageFromQueue(received, signQueueName);

        StepVerifier.create(deleteMessageResponseMono)
                .expectNextCount(1)
                .verifyComplete();
    }

    @BeforeEach
    void setup() {
        sqsAsyncClient.purgeQueue(builder -> builder.queueUrl(signQueueName));
    }

    private Mono<SendMessageResponse> sendMessageToQueue() {
        S3EventNotificationMessage createdS3ObjectDto = new S3EventNotificationMessage();
        return sqsService.send(signQueueName, createdS3ObjectDto);

    }



}
