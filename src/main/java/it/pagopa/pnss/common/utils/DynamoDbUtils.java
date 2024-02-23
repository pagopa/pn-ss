package it.pagopa.pnss.common.utils;

import lombok.CustomLog;
import reactor.util.retry.Retry;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;

import java.time.Duration;

import static it.pagopa.pnss.common.utils.LogUtils.RETRY_ATTEMPT;

@CustomLog
public class DynamoDbUtils {

    public static final Retry DYNAMO_OPTIMISTIC_LOCKING_RETRY = Retry.backoff(10, Duration.ofSeconds(3))
            .filter(ConditionalCheckFailedException.class::isInstance)
            .doBeforeRetry(retrySignal -> log.warn(RETRY_ATTEMPT, retrySignal.totalRetries(), retrySignal.failure(), retrySignal.failure().getMessage()))
            .onRetryExhaustedThrow((retrySpec, retrySignal) -> retrySignal.failure());

    private DynamoDbUtils() {
        throw new IllegalStateException("DynamoDbUtils is a utility class");
    }
}
