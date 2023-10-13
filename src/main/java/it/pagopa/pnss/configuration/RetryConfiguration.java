package it.pagopa.pnss.configuration;

import it.pagopa.pnss.configurationproperties.DynamoRetryStrategyConfiguration;
import it.pagopa.pnss.configurationproperties.UriBuilderRetryStrategyConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.util.retry.Retry;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;

import java.time.Duration;

import static it.pagopa.pnss.common.constant.Constant.RETRY_ATTEMPT;

@Configuration
@Slf4j
public class RetryConfiguration {

    @Autowired
    private DynamoRetryStrategyConfiguration dynamoRetryStrategyConfiguration;
    @Autowired
    private UriBuilderRetryStrategyConfiguration uriBuilderRetryStrategyConfiguration;

    @Bean
    Retry dynamoRetryStrategy() {
        return Retry.backoff(dynamoRetryStrategyConfiguration.maxAttempts(), Duration.ofSeconds(dynamoRetryStrategyConfiguration.minBackoff()))
                .filter(ConditionalCheckFailedException.class::isInstance)
                .doBeforeRetry(retrySignal -> log.debug(RETRY_ATTEMPT, retrySignal.totalRetries(), retrySignal.failure(), retrySignal.failure().getMessage()));
    }

    @Bean
    Retry uriBuilderRetryStrategy() {
        return Retry.backoff(uriBuilderRetryStrategyConfiguration.maxAttempts(), Duration.ofSeconds(uriBuilderRetryStrategyConfiguration.minBackoff()))
                .doBeforeRetry(retrySignal -> log.debug(RETRY_ATTEMPT, retrySignal.totalRetries(), retrySignal.failure(), retrySignal.failure().getMessage()));
    }

}
