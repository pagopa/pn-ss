package it.pagopa.pnss.configuration;

import it.pagopa.pnss.common.client.exception.DocumentKeyNotPresentException;
import it.pagopa.pnss.common.client.exception.DocumentTypeNotPresentException;
import it.pagopa.pnss.common.client.exception.IdClientNotFoundException;
import it.pagopa.pnss.configurationproperties.DynamoRetryStrategyProperties;
import it.pagopa.pnss.configurationproperties.GestoreRepositoryRetryStrategyProperties;
import it.pagopa.pnss.configurationproperties.retry.S3RetryStrategyProperties;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.util.retry.RetryBackoffSpec;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class RetryConfigurationTagsStrategyTest {

    @Test
    void realTagsRetryStrategy_retriesOnDocumentKeyNotPresentException() {
        GestoreRepositoryRetryStrategyProperties gestoreProps = new GestoreRepositoryRetryStrategyProperties(2L, 0L);
        DynamoRetryStrategyProperties dynamoProps = new DynamoRetryStrategyProperties(1L, 0L);
        S3RetryStrategyProperties s3Props = new S3RetryStrategyProperties(1L, 0L);

        RetryConfiguration retryConfiguration = new RetryConfiguration(dynamoProps, gestoreProps, s3Props);
        RetryBackoffSpec strategy = retryConfiguration.tagsRetryStrategy();

        AtomicInteger attempts = new AtomicInteger(0);

        Mono<String> mono = Mono.<String>error(new DocumentKeyNotPresentException("key"))
                .doOnSubscribe(s -> attempts.incrementAndGet())
                .retryWhen(strategy)
                .onErrorResume(e -> Mono.just("error"));

        mono.block();

        assertThat(attempts.get()).isEqualTo(3);
    }

    @Test
    void realTagsRetryStrategy_retriesOnIdClientNotFoundException() {
        GestoreRepositoryRetryStrategyProperties gestoreProps = new GestoreRepositoryRetryStrategyProperties(2L, 0L);
        DynamoRetryStrategyProperties dynamoProps = new DynamoRetryStrategyProperties(1L, 0L);
        S3RetryStrategyProperties s3Props = new S3RetryStrategyProperties(1L, 0L);

        RetryConfiguration retryConfiguration = new RetryConfiguration(dynamoProps, gestoreProps, s3Props);
        RetryBackoffSpec strategy = retryConfiguration.tagsRetryStrategy();

        AtomicInteger attempts = new AtomicInteger(0);

        Mono<String> mono = Mono.<String>error(new IdClientNotFoundException("clientId"))
                .doOnSubscribe(s -> attempts.incrementAndGet())
                .retryWhen(strategy)
                .onErrorResume(e -> Mono.just("error"));

        mono.block();

        assertThat(attempts.get()).isEqualTo(3);
    }

    @Test
    void realTagsRetryStrategy_retriesOnDocumentTypeNotPresentException() {
        GestoreRepositoryRetryStrategyProperties gestoreProps = new GestoreRepositoryRetryStrategyProperties(2L, 0L);
        DynamoRetryStrategyProperties dynamoProps = new DynamoRetryStrategyProperties(1L, 0L);
        S3RetryStrategyProperties s3Props = new S3RetryStrategyProperties(1L, 0L);

        RetryConfiguration retryConfiguration = new RetryConfiguration(dynamoProps, gestoreProps, s3Props);
        RetryBackoffSpec strategy = retryConfiguration.tagsRetryStrategy();

        AtomicInteger attempts = new AtomicInteger(0);

        Mono<String> mono = Mono.<String>error(new DocumentTypeNotPresentException("docType"))
                .doOnSubscribe(s -> attempts.incrementAndGet())
                .retryWhen(strategy)
                .onErrorResume(e -> Mono.just("error"));

        mono.block();

        assertThat(attempts.get()).isEqualTo(3);
    }

    @Test
    void realTagsRetryStrategy_doesNotRetry_onUnrelatedException() {
        GestoreRepositoryRetryStrategyProperties gestoreProps = new GestoreRepositoryRetryStrategyProperties(2L, 0L);
        DynamoRetryStrategyProperties dynamoProps = new DynamoRetryStrategyProperties(1L, 0L);
        S3RetryStrategyProperties s3Props = new S3RetryStrategyProperties(1L, 0L);

        RetryConfiguration retryConfiguration = new RetryConfiguration(dynamoProps, gestoreProps, s3Props);
        RetryBackoffSpec strategy = retryConfiguration.tagsRetryStrategy();

        AtomicInteger attempts = new AtomicInteger(0);

        Mono<String> mono = Mono.<String>error(new RuntimeException("unrelated"))
                .doOnSubscribe(s -> attempts.incrementAndGet())
                .retryWhen(strategy)
                .onErrorResume(e -> Mono.just("error"));

        mono.block();

        assertThat(attempts.get()).isEqualTo(1);
    }
}
