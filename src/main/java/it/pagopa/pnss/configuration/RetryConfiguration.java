package it.pagopa.pnss.configuration;

import it.pagopa.pnss.common.client.exception.DocumentKeyNotPresentException;
import it.pagopa.pnss.common.client.exception.DocumentTypeNotPresentException;
import it.pagopa.pnss.common.client.exception.IdClientNotFoundException;
import it.pagopa.pnss.common.exception.PatchDocumentException;
import it.pagopa.pnss.common.exception.StateMachineServiceException;
import it.pagopa.pnss.configurationproperties.DynamoRetryStrategyProperties;
import it.pagopa.pnss.configurationproperties.GestoreRepositoryRetryStrategyProperties;
import it.pagopa.pnss.configurationproperties.retry.S3RetryStrategyProperties;
import it.pagopa.pnss.configurationproperties.retry.StateMachineRetryStrategyProperties;
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
    private final DynamoRetryStrategyProperties dynamoRetryStrategyProperties;
    private final GestoreRepositoryRetryStrategyProperties gestoreRepositoryRetryStrategyProperties;
    private final S3RetryStrategyProperties s3RetryStrategyProperties;

    @Autowired
    public RetryConfiguration(DynamoRetryStrategyProperties dynamoRetryStrategyProperties, GestoreRepositoryRetryStrategyProperties gestoreRepositoryRetryStrategyProperties, S3RetryStrategyProperties s3RetryStrategyProperties) {
        this.dynamoRetryStrategyProperties = dynamoRetryStrategyProperties;
        this.gestoreRepositoryRetryStrategyProperties = gestoreRepositoryRetryStrategyProperties;
        this.s3RetryStrategyProperties = s3RetryStrategyProperties;
    }
    private final Predicate<Throwable> isNotFound = throwable -> (throwable instanceof DocumentKeyNotPresentException) || (throwable instanceof IdClientNotFoundException)  || (throwable instanceof DocumentTypeNotPresentException);

    @Bean
    RetryBackoffSpec dynamoRetryStrategy() {
        return Retry.backoff(dynamoRetryStrategyProperties.maxAttempts(), Duration.ofSeconds(dynamoRetryStrategyProperties.minBackoff()))
                .filter(DynamoDbException.class::isInstance)
                .doBeforeRetry(retrySignal -> log.info(RETRY_ATTEMPT, retrySignal.totalRetries(), retrySignal.failure().getMessage(), retrySignal.failure()))
                .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) -> retrySignal.failure());
    }

    @Bean
    RetryBackoffSpec gestoreRepositoryRetryStrategy() {
        return Retry.backoff(gestoreRepositoryRetryStrategyProperties.maxAttempts(), Duration.ofSeconds(gestoreRepositoryRetryStrategyProperties.minBackoff()))
                .filter(Predicate.not(isNotFound))
                .filter(throwable -> throwable instanceof PatchDocumentException && ((PatchDocumentException) throwable).getStatusCode().is5xxServerError())
                .doBeforeRetry(retrySignal -> log.info(RETRY_ATTEMPT, retrySignal.totalRetries(), retrySignal.failure().getMessage(), retrySignal.failure()))
                .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) -> retrySignal.failure());
    }

    @Bean
    RetryBackoffSpec s3RetryStrategy() {
        return Retry.backoff(s3RetryStrategyProperties.maxAttempts(), Duration.ofSeconds(s3RetryStrategyProperties.minBackoff()))
                .filter(S3Exception.class::isInstance)
                .filter(Predicate.not(NoSuchKeyException.class::isInstance))
                .doBeforeRetry(retrySignal -> log.info(RETRY_ATTEMPT, retrySignal.totalRetries(), retrySignal.failure().getMessage(), retrySignal.failure()))
                .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) -> retrySignal.failure());
    }


    @Bean
    RetryBackoffSpec smRetryStrategy(StateMachineRetryStrategyProperties smRetryStrategyProperties) {
        return Retry.backoff(smRetryStrategyProperties.maxAttempts(), Duration.ofSeconds(smRetryStrategyProperties.minBackoff()))
                .filter(StateMachineServiceException.class::isInstance)
                .doBeforeRetry(retrySignal -> log.info(RETRY_ATTEMPT, retrySignal.totalRetries(), retrySignal.failure().getMessage(), retrySignal.failure().getCause()))
                .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) -> retrySignal.failure());
    }
}
