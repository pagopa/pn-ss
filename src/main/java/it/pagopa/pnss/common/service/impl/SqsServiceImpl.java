package it.pagopa.pnss.common.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.commons.utils.MDCUtils;
import it.pagopa.pnss.common.exception.SqsClientException;
import it.pagopa.pnss.common.model.pojo.SqsMessageWrapper;
import it.pagopa.pnss.common.service.SqsService;
import it.pagopa.pnss.common.utils.JsonUtils;
import lombok.CustomLog;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;
import reactor.util.retry.RetryBackoffSpec;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.*;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;

import static it.pagopa.pnss.common.utils.LogUtils.*;

@Service
@CustomLog
public class SqsServiceImpl implements SqsService {

    private final SqsAsyncClient sqsAsyncClient;
    private final ObjectMapper objectMapper;
    private final RetryBackoffSpec sqsRetryStrategy;
    private final JsonUtils jsonUtils;
    private Integer maxMessages;

    public SqsServiceImpl(SqsAsyncClient sqsAsyncClient, ObjectMapper objectMapper, JsonUtils jsonUtils) {
        this.sqsAsyncClient = sqsAsyncClient;
        this.objectMapper = objectMapper;
        this.jsonUtils = jsonUtils;
        this.sqsRetryStrategy = Retry.backoff(3, Duration.ofMillis(100)) //TODO blue phase: check retry strategy
                .filter(SqsException.class::isInstance)
                .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) -> retrySignal.failure());
        this.maxMessages = 10; //TODO blue phase: check max messages
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

    @Override
    public <T> Flux<SqsMessageWrapper<T>> getMessages(String queueName, Class<T> messageContentClass) {
        AtomicInteger actualMessages = new AtomicInteger();
        AtomicBoolean listIsEmpty = new AtomicBoolean();
        listIsEmpty.set(false);

        BooleanSupplier condition = () -> (actualMessages.get() <= maxMessages && !listIsEmpty.get());

        return getQueueUrlFromName(queueName).flatMap(queueUrl -> Mono.fromCompletionStage(sqsAsyncClient.receiveMessage(builder -> builder.queueUrl(
                        queueUrl))))
                .retryWhen(getSqsRetryStrategy())
                .flatMap(receiveMessageResponse ->
                        {
                            var messages = receiveMessageResponse.messages();
                            if (messages.isEmpty())
                                listIsEmpty.set(true);
                            return Mono.justOrEmpty(messages);
                        }
                )
                .flatMapMany(Flux::fromIterable)
                .map(message ->
                {
                    actualMessages.incrementAndGet();
                    return new SqsMessageWrapper<>(message,
                            jsonUtils.convertJsonStringToObject(message.body(),
                                    messageContentClass));
                })
                .onErrorResume(throwable -> {
                    log.error(throwable.getMessage(), throwable);
                    return Mono.error(new SqsClientException(queueName));
                })
                .repeat(condition);
    }
    private RetryBackoffSpec getSqsRetryStrategy() {
        var mdcContextMap = MDCUtils.retrieveMDCContextMap();
        return sqsRetryStrategy.doBeforeRetry(retrySignal -> {
            MDCUtils.enrichWithMDC(null, mdcContextMap);
            log.debug(SHORT_RETRY_ATTEMPT, retrySignal.totalRetries(), retrySignal.failure(), retrySignal.failure().getMessage());
        });
    }

    @Override
    public Mono<DeleteMessageResponse> deleteMessageFromQueue(Message message, String queueName)  {
        return getQueueUrlFromName(queueName).doOnSuccess(queueUrl -> log.debug("Delete message with id {} from {} queue",
                        message.messageId(),
                        queueName))
                .flatMap(queueUrl -> Mono.fromCompletionStage(sqsAsyncClient.deleteMessage(builder -> builder.queueUrl(
                        queueUrl).receiptHandle(message.receiptHandle()))))
                .retryWhen(getSqsRetryStrategy())
                .onErrorResume(throwable -> {
                    log.error(throwable.getMessage(), throwable);
                    return Mono.error(new SqsClientException(queueName));
                });
    }

    private Mono<String> getQueueUrlFromName(final String queueName) {
        return Mono.fromCompletionStage(sqsAsyncClient.getQueueUrl(builder -> builder.queueName(queueName)))
                .map(GetQueueUrlResponse::queueUrl);
    }

}
