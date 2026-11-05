package it.pagopa.pnss.configuration;

import it.pagopa.pnss.configurationproperties.PnSsConfig;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import reactor.core.publisher.Mono;
import reactor.util.retry.RetryBackoffSpec;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.core.exception.ApiCallTimeoutException;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class RetryConfigurationS3StrategyTest {

    private static final long MAX_ATTEMPTS = 2L;

    @ParameterizedTest(name = "{0}")
    @MethodSource("retryableErrors")
    void realS3RetryStrategy_retriesOnTransientS3Errors(String description, Throwable error) {
        assertThat(attempts(error)).isEqualTo(MAX_ATTEMPTS + 1);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("nonRetryableErrors")
    void realS3RetryStrategy_doesNotRetry_onClientOrNonS3Errors(String description, Throwable error) {
        assertThat(attempts(error)).isEqualTo(1);
    }


    private static PnSsConfig buildPnSsConfig() {
        PnSsConfig pnSsConfig = new PnSsConfig();

        PnSsConfig.S3 s3 = new PnSsConfig.S3();
        PnSsConfig.S3.RetryStrategy s3RetryStrategy = new PnSsConfig.S3.RetryStrategy();
        s3RetryStrategy.setMaxAttempts(MAX_ATTEMPTS);
        s3RetryStrategy.setMinBackoff(0L);
        s3.setRetryStrategy(s3RetryStrategy);
        pnSsConfig.setS3(s3);

        return pnSsConfig;
    }

    private static Throwable s3Exception(int statusCode, String errorCode) {
        return S3Exception.builder()
                .statusCode(statusCode)
                .awsErrorDetails(AwsErrorDetails.builder().errorCode(errorCode).errorMessage(errorCode).build())
                .build();
    }

    private static int attempts(Throwable error) {
        RetryBackoffSpec strategy = new RetryConfiguration(buildPnSsConfig()).s3RetryStrategy();

        AtomicInteger attempts = new AtomicInteger(0);

        Mono<String> mono = Mono.<String>error(error)
                .doOnSubscribe(s -> attempts.incrementAndGet())
                .retryWhen(strategy)
                .onErrorResume(e -> Mono.just("error"));

        mono.block();

        return attempts.get();
    }

    static Stream<Arguments> retryableErrors() {
        return Stream.of(
                Arguments.of("500 InternalError", s3Exception(500, "InternalError")),
                Arguments.of("503 SlowDown", s3Exception(503, "SlowDown")),
                Arguments.of("429 TooManyRequests", s3Exception(429, "TooManyRequests")),
                Arguments.of("400 Throttling", s3Exception(400, "Throttling")));
    }

    static Stream<Arguments> nonRetryableErrors() {
        return Stream.of(
                Arguments.of("400 InvalidArgument (retention nel passato)", s3Exception(400, "InvalidArgument")),
                Arguments.of("403 AccessDenied", s3Exception(403, "AccessDenied")),
                Arguments.of("409 RestoreAlreadyInProgress", s3Exception(409, "RestoreAlreadyInProgress")),
                Arguments.of("404 NoSuchKey", NoSuchKeyException.builder().statusCode(404).build()),
                Arguments.of("404 NoSuchBucket", NoSuchBucketException.builder().statusCode(404).build()),
                Arguments.of("SdkClientException", SdkClientException.create("Unable to execute HTTP request")),
                Arguments.of("ApiCallTimeoutException", ApiCallTimeoutException.create(1000)),
                Arguments.of("RuntimeException", new RuntimeException("unrelated")));
    }
}
