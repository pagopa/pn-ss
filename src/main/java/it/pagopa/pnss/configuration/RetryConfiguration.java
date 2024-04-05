package it.pagopa.pnss.configuration;

import it.pagopa.pnss.common.client.exception.DocumentKeyNotPresentException;
import it.pagopa.pnss.common.client.exception.DocumentTypeNotPresentException;
import it.pagopa.pnss.common.client.exception.IdClientNotFoundException;
import it.pagopa.pnss.configurationproperties.DynamoRetryStrategyProperties;
import it.pagopa.pnss.configurationproperties.GestoreRepositoryRetryStrategyProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.util.retry.Retry;
import reactor.util.retry.RetryBackoffSpec;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.time.Duration;
import java.util.function.Predicate;

import static it.pagopa.pnss.common.utils.LogUtils.RETRY_ATTEMPT;

@Configuration
@Slf4j
public class RetryConfiguration {

    @Autowired
    private DynamoRetryStrategyProperties dynamoRetryStrategyProperties;
    @Autowired
    private GestoreRepositoryRetryStrategyProperties gestoreRepositoryRetryStrategyProperties;

    private final Predicate<Throwable> isNotFound = throwable -> (throwable instanceof DocumentKeyNotPresentException) || (throwable instanceof IdClientNotFoundException)  || (throwable instanceof DocumentTypeNotPresentException);

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
                .filter(Predicate.not(isNotFound))
                .doBeforeRetry(retrySignal -> log.debug(RETRY_ATTEMPT, retrySignal.totalRetries(), retrySignal.failure(), retrySignal.failure().getMessage()))
                .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) -> retrySignal.failure());
    }

    @Bean
    RetryBackoffSpec s3RetryStrategy() {
        return Retry.backoff(gestoreRepositoryRetryStrategyProperties.maxAttempts(), Duration.ofSeconds(gestoreRepositoryRetryStrategyProperties.minBackoff()))
                .filter(S3Exception.class::isInstance)
                .filter(Predicate.not(NoSuchKeyException.class::isInstance))
                .doBeforeRetry(retrySignal -> log.debug(RETRY_ATTEMPT, retrySignal.totalRetries(), retrySignal.failure(), retrySignal.failure().getMessage()))
                .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) -> retrySignal.failure());
    }
}
