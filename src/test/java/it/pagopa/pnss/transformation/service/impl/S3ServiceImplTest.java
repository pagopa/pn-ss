package it.pagopa.pnss.transformation.service.impl;

import it.pagopa.pnss.configuration.RetryConfiguration;
import it.pagopa.pnss.configurationproperties.PnSsConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.test.StepVerifier;
import reactor.util.retry.RetryBackoffSpec;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.ObjectLockRetention;
import software.amazon.awssdk.services.s3.model.ObjectLockRetentionMode;
import software.amazon.awssdk.services.s3.model.RestoreRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class S3ServiceImplTest {

    private static final String KEY = "documentKey";
    private static final String BUCKET = "bucket";
    private static final Duration NO_HANG = Duration.ofSeconds(5);

    @Mock
    private S3AsyncClient s3AsyncClient;

    @Mock
    private S3Presigner s3Presigner;

    private S3ServiceImpl s3Service;

    @BeforeEach
    void setUp() {
        PnSsConfig pnSsConfig = new PnSsConfig();
        PnSsConfig.S3 s3 = new PnSsConfig.S3();
        PnSsConfig.S3.RetryStrategy s3RetryStrategy = new PnSsConfig.S3.RetryStrategy();
        s3RetryStrategy.setMaxAttempts(2L);
        s3RetryStrategy.setMinBackoff(1L);
        s3.setRetryStrategy(s3RetryStrategy);
        pnSsConfig.setS3(s3);

        RetryBackoffSpec strategy = ReflectionTestUtils.invokeMethod(new RetryConfiguration(pnSsConfig), "s3RetryStrategy");
        s3Service = new S3ServiceImpl(s3AsyncClient, s3Presigner, strategy);
    }

    @Test
    @SuppressWarnings("unchecked")
    void putObjectRetention_whenRetentionDateInPast_failsWithoutRetry() {
        when(s3AsyncClient.putObjectRetention(any(Consumer.class)))
                .thenReturn(CompletableFuture.failedFuture(s3Exception(400, "InvalidArgument")));

        StepVerifier.withVirtualTime(() -> s3Service.putObjectRetention(KEY, BUCKET, objectLockRetention()))
                .expectSubscription()
                .expectErrorMatches(e -> e instanceof S3Exception s3e && s3e.statusCode() == 400)
                .verify(NO_HANG);
    }

    @Test
    @SuppressWarnings("unchecked")
    void putObjectRetention_whenS3Unavailable_retriesWithBackoff() {
        when(s3AsyncClient.putObjectRetention(any(Consumer.class)))
                .thenReturn(CompletableFuture.failedFuture(s3Exception(503, "SlowDown")));

        StepVerifier.withVirtualTime(() -> s3Service.putObjectRetention(KEY, BUCKET, objectLockRetention()))
                .expectSubscription()
                .expectNoEvent(Duration.ofSeconds(1))
                .thenAwait(Duration.ofSeconds(10))
                .expectErrorMatches(e -> e instanceof S3Exception s3e && s3e.statusCode() == 503)
                .verify(NO_HANG);
    }

    @Test
    @SuppressWarnings("unchecked")
    void headObject_whenKeyNotFound_failsWithoutRetry() {
        when(s3AsyncClient.headObject(any(Consumer.class)))
                .thenReturn(CompletableFuture.failedFuture(NoSuchKeyException.builder().statusCode(404).build()));

        StepVerifier.withVirtualTime(() -> s3Service.headObject(KEY, BUCKET))
                .expectSubscription()
                .expectError(NoSuchKeyException.class)
                .verify(NO_HANG);
    }

    @Test
    @SuppressWarnings("unchecked")
    void headObject_whenSdkClientException_failsWithoutRetry() {
        when(s3AsyncClient.headObject(any(Consumer.class)))
                .thenReturn(CompletableFuture.failedFuture(SdkClientException.create("Unable to execute HTTP request")));

        StepVerifier.withVirtualTime(() -> s3Service.headObject(KEY, BUCKET))
                .expectSubscription()
                .expectError(SdkClientException.class)
                .verify(NO_HANG);
    }

    @Test
    @SuppressWarnings("unchecked")
    void restoreObject_whenRestoreAlreadyInProgress_failsWithoutRetry() {
        when(s3AsyncClient.restoreObject(any(Consumer.class)))
                .thenReturn(CompletableFuture.failedFuture(s3Exception(409, "RestoreAlreadyInProgress")));

        StepVerifier.withVirtualTime(() -> s3Service.restoreObject(KEY, BUCKET, RestoreRequest.builder().days(1).build()))
                .expectSubscription()
                .expectErrorMatches(e -> e instanceof S3Exception s3e && s3e.statusCode() == 409)
                .verify(NO_HANG);
    }

    private static S3Exception s3Exception(int statusCode, String errorCode) {
        return (S3Exception) S3Exception.builder()
                .statusCode(statusCode)
                .awsErrorDetails(AwsErrorDetails.builder().errorCode(errorCode).errorMessage(errorCode).build())
                .build();
    }

    private static ObjectLockRetention objectLockRetention() {
        return ObjectLockRetention.builder()
                .mode(ObjectLockRetentionMode.GOVERNANCE)
                .retainUntilDate(Instant.parse("2020-01-01T00:00:00Z"))
                .build();
    }
}
