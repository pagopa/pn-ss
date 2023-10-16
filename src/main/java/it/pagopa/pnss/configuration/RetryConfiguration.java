package it.pagopa.pnss.configuration;

import it.pagopa.pnss.configurationproperties.ArubaRetryStrategyProperties;
import it.pagopa.pnss.configurationproperties.DynamoRetryStrategyProperties;
import it.pagopa.pnss.configurationproperties.GestoreRepositoryRetryStrategyProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.util.retry.Retry;
import reactor.util.retry.RetryBackoffSpec;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.time.Duration;

import static it.pagopa.pnss.common.constant.Constant.RETRY_ATTEMPT;

@Configuration
@Slf4j
public class RetryConfiguration {

    @Autowired
    private DynamoRetryStrategyProperties dynamoRetryStrategyProperties;
    @Autowired
    private GestoreRepositoryRetryStrategyProperties gestoreRepositoryRetryStrategyProperties;
    @Autowired
    private ArubaRetryStrategyProperties arubaRetryStrategyProperties;

    @Bean
    RetryBackoffSpec dynamoRetryStrategy() {
        return Retry.backoff(dynamoRetryStrategyProperties.maxAttempts(), Duration.ofSeconds(dynamoRetryStrategyProperties.minBackoff()))
                .filter(DynamoDbException.class::isInstance)
                .doBeforeRetry(retrySignal -> log.debug(RETRY_ATTEMPT, retrySignal.totalRetries(), retrySignal.failure(), retrySignal.failure().getMessage()))
                .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) -> retrySignal.failure());
    }

    @Bean
    RetryBackoffSpec gestoreRepositoryRetryStrategy() {
        return Retry.backoff(gestoreRepositoryRetryStrategyProperties.maxAttempts(), Duration.ofSeconds(gestoreRepositoryRetryStrategyProperties.minBackoff()))
                .doBeforeRetry(retrySignal -> log.debug(RETRY_ATTEMPT, retrySignal.totalRetries(), retrySignal.failure(), retrySignal.failure().getMessage()))
                .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) -> retrySignal.failure());
    }

    @Bean
    RetryBackoffSpec s3RetryStrategy() {
        return Retry.backoff(gestoreRepositoryRetryStrategyProperties.maxAttempts(), Duration.ofSeconds(gestoreRepositoryRetryStrategyProperties.minBackoff()))
                .filter(S3Exception.class::isInstance)
                .doBeforeRetry(retrySignal -> log.debug(RETRY_ATTEMPT, retrySignal.totalRetries(), retrySignal.failure(), retrySignal.failure().getMessage()))
                .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) -> retrySignal.failure());
    }

    @Bean
    RetryBackoffSpec arubaRetryStrategy() {
        return Retry.backoff(arubaRetryStrategyProperties.maxAttempts(), Duration.ofSeconds(arubaRetryStrategyProperties.minBackoff()))
                .doBeforeRetry(retrySignal -> log.debug(RETRY_ATTEMPT, retrySignal.totalRetries(), retrySignal.failure(), retrySignal.failure().getMessage()))
                .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) -> retrySignal.failure());
    }

}
