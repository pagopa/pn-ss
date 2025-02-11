package it.pagopa.pnss.common.service;

import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequestEntry;
import software.amazon.awssdk.services.eventbridge.model.PutEventsResponse;

public interface EventBridgeService {

    Mono<PutEventsResponse> putSingleEvent(PutEventsRequestEntry event);

}
