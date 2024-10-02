package it.pagopa.pnss.common.service;


import it.pagopa.pnss.testutils.annotation.SpringBootTestWebEnv;
import it.pagopa.pnss.transformation.model.dto.CreatedS3ObjectDto;
import lombok.CustomLog;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;

@SpringBootTestWebEnv
@AutoConfigureWebTestClient
@CustomLog
public class SqsServiceTest {
    @Autowired
    SqsService sqsService;
    @Value("${s3.queue.sign-queue-name}")
    private String signQueueName;

    @Test
    public void testSendOk() {
        CreatedS3ObjectDto createdS3ObjectDto = new CreatedS3ObjectDto();
        log.info(signQueueName);

       StepVerifier.create(sqsService.send(signQueueName, createdS3ObjectDto))
               .expectNextCount(1)
                .verifyComplete();
    }

    @Test
    public void testSendWrongQueueName() {
        CreatedS3ObjectDto createdS3ObjectDto = new CreatedS3ObjectDto();
        log.info(signQueueName);
        String signQueueName = "signQueueName";
        StepVerifier.create(sqsService.send(signQueueName, createdS3ObjectDto))
                .expectError()
                .verify();
    }

    @Test
    public void getMessages() {
        sendMessageToQueue().block();

        StepVerifier.create(sqsService.getMessages(signQueueName, CreatedS3ObjectDto.class))
                .expectNextCount(1)
                .verifyComplete();
    }

    @Test
    public void deleteMessageFromQueue() {
        SendMessageResponse sendMessageResponseMono = sendMessageToQueue().block();

        Message message = Message.builder().messageId(sendMessageResponseMono.messageId()).build();

        assert sendMessageResponseMono != null;
        StepVerifier.create(sqsService.deleteMessageFromQueue(message, signQueueName))
                .expectNextCount(1)
                .verifyComplete();
    }

    private Mono<SendMessageResponse> sendMessageToQueue() {
        CreatedS3ObjectDto createdS3ObjectDto = new CreatedS3ObjectDto();
        return sqsService.send(signQueueName, createdS3ObjectDto);

    }



}
