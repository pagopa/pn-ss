package it.pagopa.pnss.common.service;

import it.pagopa.pnss.common.exception.SqsClientException;
import it.pagopa.pnss.common.model.pojo.SqsMessageWrapper;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.sqs.model.DeleteMessageResponse;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;

public interface SqsService {

    <T> Mono<SendMessageResponse> send(final String queueName, final T queuePayload) throws SqsClientException;

    <T> Flux<SqsMessageWrapper<T>> getMessages(final String queueName, final Class<T> messageContentClass, final Integer maxMessages);

    Mono<DeleteMessageResponse> deleteMessageFromQueue(final Message message, final String queueName);
}
