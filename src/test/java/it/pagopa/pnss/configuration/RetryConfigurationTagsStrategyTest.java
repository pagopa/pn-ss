package it.pagopa.pnss.configuration;

import it.pagopa.pnss.common.client.exception.DocumentKeyNotPresentException;
import it.pagopa.pnss.common.client.exception.DocumentTypeNotPresentException;
import it.pagopa.pnss.common.client.exception.IdClientNotFoundException;
import it.pagopa.pnss.configurationproperties.PnSsConfig;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.util.retry.RetryBackoffSpec;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class RetryConfigurationTagsStrategyTest {

    private static PnSsConfig buildPnSsConfig() {
        PnSsConfig pnSsConfig = new PnSsConfig();

        PnSsConfig.Dynamo dynamo = new PnSsConfig.Dynamo();
        PnSsConfig.Dynamo.RetryStrategy dynamoRetryStrategy = new PnSsConfig.Dynamo.RetryStrategy();
        dynamoRetryStrategy.setMaxAttempts(1L);
        dynamoRetryStrategy.setMinBackoff(0L);
        dynamo.setRetryStrategy(dynamoRetryStrategy);
        pnSsConfig.setDynamo(dynamo);

        PnSsConfig.GestoreRepository gestoreRepository = new PnSsConfig.GestoreRepository();
        PnSsConfig.GestoreRepository.RetryStrategy gestoreRetryStrategy = new PnSsConfig.GestoreRepository.RetryStrategy();
        gestoreRetryStrategy.setMaxAttempts(2L);
        gestoreRetryStrategy.setMinBackoff(0L);
        gestoreRepository.setRetryStrategy(gestoreRetryStrategy);
        pnSsConfig.setGestoreRepository(gestoreRepository);

        PnSsConfig.S3 s3 = new PnSsConfig.S3();
        PnSsConfig.S3.RetryStrategy s3RetryStrategy = new PnSsConfig.S3.RetryStrategy();
        s3RetryStrategy.setMaxAttempts(1L);
        s3RetryStrategy.setMinBackoff(0L);
        s3.setRetryStrategy(s3RetryStrategy);
        pnSsConfig.setS3(s3);

        return pnSsConfig;
    }

    @Test
    void realTagsRetryStrategy_retriesOnDocumentKeyNotPresentException() {
        PnSsConfig pnSsConfig = buildPnSsConfig();

        RetryConfiguration retryConfiguration = new RetryConfiguration(pnSsConfig);
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
        PnSsConfig pnSsConfig = buildPnSsConfig();

        RetryConfiguration retryConfiguration = new RetryConfiguration(pnSsConfig);
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
        PnSsConfig pnSsConfig = buildPnSsConfig();

        RetryConfiguration retryConfiguration = new RetryConfiguration(pnSsConfig);
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
        PnSsConfig pnSsConfig = buildPnSsConfig();

        RetryConfiguration retryConfiguration = new RetryConfiguration(pnSsConfig);
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
