package it.pagopa.pnss.common.service.impl;

import it.pagopa.pnss.common.service.EventBridgeService;
import lombok.CustomLog;
import org.apache.commons.lang3.NotImplementedException;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.eventbridge.EventBridgeAsyncClient;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequestEntry;
import software.amazon.awssdk.services.eventbridge.model.PutEventsResponse;

@Service
@CustomLog
public class EventBridgeServiceImpl implements EventBridgeService {

    private final EventBridgeAsyncClient eventBridgeAsyncClient;

    public EventBridgeServiceImpl(EventBridgeAsyncClient eventBridgeAsyncClient) {
        this.eventBridgeAsyncClient = eventBridgeAsyncClient;
    }

    @Override
    public Mono<PutEventsResponse> putSingleEvent(PutEventsRequestEntry event) {
        throw new NotImplementedException();
    }
}
