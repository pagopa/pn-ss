package it.pagopa.pnss.common.service;

import it.pagopa.pnss.common.exception.SqsClientException;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;

public interface SqsService {

    <T> Mono<SendMessageResponse> send(final String queueName, final T queuePayload) throws SqsClientException;

}
