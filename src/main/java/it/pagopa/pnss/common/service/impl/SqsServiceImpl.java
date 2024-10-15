package it.pagopa.pnss.common.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pnss.common.exception.SqsClientException;
import it.pagopa.pnss.common.service.SqsService;
import lombok.CustomLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlResponse;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;

import static it.pagopa.pnss.common.utils.LogUtils.INSERTED_DATA_IN_SQS;
import static it.pagopa.pnss.common.utils.LogUtils.INSERTING_DATA_IN_SQS;

@Service
@CustomLog
public class SqsServiceImpl implements SqsService {

    private final SqsAsyncClient sqsAsyncClient;
    private final ObjectMapper objectMapper;

    public SqsServiceImpl(SqsAsyncClient sqsAsyncClient, ObjectMapper objectMapper) {
        this.sqsAsyncClient = sqsAsyncClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public <T> Mono<SendMessageResponse> send(String queueName, T queuePayload) throws SqsClientException {
        log.debug(INSERTING_DATA_IN_SQS, queuePayload, queueName);
        return Mono.fromCallable(() -> objectMapper.writeValueAsString(queuePayload))
                .doOnSuccess(sendMessageResponse -> log.info("Try to publish on {} with payload {}", queueName, sendMessageResponse))
                .zipWith(getQueueUrlFromName(queueName))
                .flatMap(objects -> Mono.fromCompletionStage(sqsAsyncClient.sendMessage(builder -> builder.queueUrl(objects.getT2())
                        .messageBody(objects.getT1()))))
                .onErrorResume(throwable -> {
                    log.error("Error on sqs publish : {}", throwable.getMessage(), throwable);
                    return Mono.error(new SqsClientException(queueName));
                })
                .doOnSuccess(result -> log.info(INSERTED_DATA_IN_SQS, queueName));
    }

    private Mono<String> getQueueUrlFromName(final String queueName) {
        return Mono.fromCompletionStage(sqsAsyncClient.getQueueUrl(builder -> builder.queueName(queueName)))
                .map(GetQueueUrlResponse::queueUrl);
    }

}
