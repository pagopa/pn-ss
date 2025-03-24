package it.pagopa.pnss.common.service.impl;

import it.pagopa.pnss.common.service.EventBridgeService;
import lombok.CustomLog;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;
import software.amazon.awssdk.services.eventbridge.EventBridgeAsyncClient;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequestEntry;
import software.amazon.awssdk.services.eventbridge.model.PutEventsResponse;

import java.time.Duration;
import static it.pagopa.pnss.common.utils.LogUtils.*;

@Service
@CustomLog
public class EventBridgeServiceImpl implements EventBridgeService {

    private final EventBridgeAsyncClient eventBridgeAsyncClient;


    public EventBridgeServiceImpl(EventBridgeAsyncClient eventBridgeAsyncClient) {
        this.eventBridgeAsyncClient = eventBridgeAsyncClient;
    }

    @Override
    public Mono<PutEventsResponse> putSingleEvent(PutEventsRequestEntry event) {
        log.info(CLIENT_METHOD_INVOCATION_WITH_ARGS, EVENT_BRIDGE_PUT_SINGLE_EVENT, event);
            return Mono.fromCompletionStage(eventBridgeAsyncClient.putEvents(builder -> builder.entries(event)))
                    .doOnError(throwable -> log.error("EventBridgeClient error ---> {}", throwable.getMessage(), throwable.getCause()))
                    .doOnSuccess(result -> log.info(CLIENT_METHOD_RETURN, EVENT_BRIDGE_PUT_SINGLE_EVENT, result))
                    .retryWhen(Retry.backoff(3, Duration.ofSeconds(2)));
    }
}



