package it.pagopa.pnss.transformation.service.impl;

import it.pagopa.pnss.configurationproperties.retry.S3RetryStrategyProperties;
import it.pagopa.pnss.transformation.service.S3Service;
import lombok.CustomLog;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;
import reactor.util.retry.RetryBackoffSpec;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.time.Duration;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static it.pagopa.pnss.common.utils.LogUtils.*;

@Service
@CustomLog
public class S3ServiceImpl implements S3Service {

    private final S3AsyncClient s3AsyncClient;
    private final S3Presigner s3Presigner;
    private final RetryBackoffSpec s3RetryStrategy;
    private final Predicate<Throwable> isRestoreAlreadyInProgress = throwable -> (throwable instanceof AwsServiceException) && ((AwsServiceException) throwable).awsErrorDetails().errorCode().equalsIgnoreCase("RestoreAlreadyInProgress");


    public S3ServiceImpl(S3AsyncClient s3AsyncClient, S3Presigner s3Presigner, S3RetryStrategyProperties s3RetryStrategyProperties) {
        this.s3AsyncClient = s3AsyncClient;
        this.s3Presigner = s3Presigner;
        s3RetryStrategy = Retry.backoff(s3RetryStrategyProperties.maxAttempts(), Duration.ofSeconds(s3RetryStrategyProperties.minBackoff()))
                .filter(S3Exception.class::isInstance)
                .doBeforeRetry(retrySignal -> log.debug(RETRY_ATTEMPT, retrySignal.totalRetries(), retrySignal.failure(), retrySignal.failure().getMessage()))
                .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) -> retrySignal.failure());
    }

    @Override
    public Mono<ResponseBytes<GetObjectResponse>> getObject(String key, String bucketName) {
        log.debug(CLIENT_METHOD_INVOCATION, GET_OBJECT, Stream.of(key, bucketName).toList());
        return Mono.fromCompletionStage(s3AsyncClient.getObject(builder -> builder.key(key).bucket(bucketName),
                                                                AsyncResponseTransformer.toBytes()))
                   .doOnNext(getObjectResponseResponseBytes -> log.info(CLIENT_METHOD_RETURN, GET_OBJECT, key))
                   .retryWhen(s3RetryStrategy);
    }

    @Override
    public Mono<PutObjectResponse> putObject(String key, byte[] fileBytes, String bucketName) {
        log.debug(CLIENT_METHOD_INVOCATION, PUT_OBJECT, Stream.of(key, bucketName).toList());
        return Mono.fromCallable(() -> new String(Base64.encodeBase64(DigestUtils.md5(fileBytes))))
                   .flatMap(contentMD5 -> Mono.fromCompletionStage(s3AsyncClient.putObject(builder -> builder.key(key)
                                                                                                             .contentMD5(contentMD5)
                                                                                                             .bucket(bucketName),
                                                                                           AsyncRequestBody.fromBytes(fileBytes))))
                   .doOnNext(putObjectResponse -> log.info(CLIENT_METHOD_RETURN, PUT_OBJECT, putObjectResponse))
                   .retryWhen(s3RetryStrategy)
                   .doOnError(throwable -> log.warn(CLIENT_METHOD_RETURN_WITH_ERROR, PUT_OBJECT, throwable, throwable.getMessage()));


    }

    @Override
    public Mono<DeleteObjectResponse> deleteObject(String key, String bucketName) {
        log.debug(CLIENT_METHOD_INVOCATION, DELETE_OBJECT, Stream.of(key, bucketName).toList());
        return Mono.fromCompletionStage(s3AsyncClient.deleteObject(builder -> builder.key(key).bucket(bucketName)))
                   .doOnNext(deleteObjectResponse -> log.info(CLIENT_METHOD_RETURN, DELETE_OBJECT, deleteObjectResponse))
                   .retryWhen(s3RetryStrategy)
                   .doOnError(throwable -> log.warn(CLIENT_METHOD_RETURN_WITH_ERROR, DELETE_OBJECT, throwable, throwable.getMessage()));
    }

    @Override
    public Mono<RestoreObjectResponse> restoreObject(String key, String bucketName, RestoreRequest restoreRequest) {
        log.debug(CLIENT_METHOD_INVOCATION, RESTORE_OBJECT, Stream.of(key, bucketName, restoreRequest).toList());
        return Mono.fromCompletionStage(s3AsyncClient.restoreObject(builder -> builder.key(key).bucket(bucketName).restoreRequest(restoreRequest)))
                .doOnNext(restoreObjectResponse -> log.info(CLIENT_METHOD_RETURN, RESTORE_OBJECT, restoreObjectResponse))
                .retryWhen(s3RetryStrategy.filter(isRestoreAlreadyInProgress.negate()))
                .doOnError(isRestoreAlreadyInProgress.negate(), throwable -> log.warn(CLIENT_METHOD_RETURN_WITH_ERROR, RESTORE_OBJECT, throwable, throwable.getMessage()));
    }

    @Override
    public Mono<PresignedGetObjectRequest> presignGetObject(GetObjectRequest getObjectRequest, Duration duration) {
        log.debug(CLIENT_METHOD_INVOCATION, PRESIGN_GET_OBJECT, Stream.of(getObjectRequest, duration).toList());
        return Mono.just(s3Presigner.presignGetObject(builder -> builder.getObjectRequest(getObjectRequest).signatureDuration(duration)))
                .doOnNext(presignedGetObjectRequest -> log.info(CLIENT_METHOD_RETURN, PRESIGN_GET_OBJECT, presignedGetObjectRequest))
                .retryWhen(s3RetryStrategy);
    }

    @Override
    public Mono<GetBucketLifecycleConfigurationResponse> getBucketLifecycleConfiguration(String bucketName) {
            log.debug(CLIENT_METHOD_INVOCATION, GET_BUCKET_LIFECYCLE_CONFIGURATION, bucketName);
            return Mono.fromCompletionStage(s3AsyncClient.getBucketLifecycleConfiguration(builder -> builder.bucket(bucketName)))
                    .doOnNext(response -> log.info(CLIENT_METHOD_RETURN, GET_BUCKET_LIFECYCLE_CONFIGURATION, response))
                    .retryWhen(s3RetryStrategy);

    }

    @Override
    public Mono<HeadObjectResponse> headObject(String key, String bucketName) {
        log.debug(CLIENT_METHOD_INVOCATION, HEAD_OBJECT, Stream.of(key, bucketName).toList());
        return Mono.fromCompletionStage(s3AsyncClient.headObject(builder -> builder.key(key).bucket(bucketName).checksumMode(ChecksumMode.ENABLED)))
                .doOnNext(headObjectResponse -> log.info(CLIENT_METHOD_RETURN, HEAD_OBJECT, headObjectResponse))
                .retryWhen(s3RetryStrategy.filter(throwable -> !(throwable instanceof NoSuchKeyException)));
    }

    @Override
    public Mono<PutObjectRetentionResponse> putObjectRetention(String key, String bucketName, ObjectLockRetention objectLockRetention) {
        log.debug(CLIENT_METHOD_INVOCATION, PUT_OBJECT_RETENTION, Stream.of(key, bucketName, objectLockRetention).toList());
        return Mono.fromCompletionStage(s3AsyncClient.putObjectRetention(builder -> builder.key(key).bucket(bucketName).retention(objectLockRetention)))
                .doOnNext(putObjectRetentionResponse -> log.info(CLIENT_METHOD_RETURN, PUT_OBJECT_RETENTION, putObjectRetentionResponse))
                .retryWhen(s3RetryStrategy);
    }

    @Override
    public Mono<PutObjectTaggingResponse> putObjectTagging(String key, String bucketName, Tagging tagging) {
        log.debug(CLIENT_METHOD_INVOCATION, PUT_OBJECT_TAGGING, Stream.of(key, bucketName, tagging).toList());
        return Mono.fromCompletionStage(s3AsyncClient.putObjectTagging(builder -> builder.key(key).bucket(bucketName).tagging(tagging)))
                .doOnNext(putObjectTaggingResponse ->  log.info(CLIENT_METHOD_RETURN, PUT_OBJECT_TAGGING, putObjectTaggingResponse))
                .retryWhen(s3RetryStrategy);
    }

    @Override
    public Mono<GetObjectTaggingResponse> getObjectTagging(String key, String bucketName) {
        log.debug(CLIENT_METHOD_INVOCATION, GET_OBJECT_TAGGING, Stream.of(key, bucketName).toList());
        return Mono.fromCompletionStage(s3AsyncClient.getObjectTagging(builder -> builder.key(key).bucket(bucketName)))
                .doOnNext(getObjectTaggingResponse ->  log.info(CLIENT_METHOD_RETURN, GET_OBJECT_TAGGING, getObjectTaggingResponse))
                .retryWhen(s3RetryStrategy);
    }

    @Override
    public Mono<DeleteObjectTaggingResponse> deleteObjectTagging(String key, String bucketName, Tagging tagging) {
        log.debug(CLIENT_METHOD_INVOCATION, DELETE_OBJECT_TAGGING, Stream.of(key, bucketName, tagging).toList());
        return Mono.fromCompletionStage(s3AsyncClient.deleteObjectTagging(builder -> builder.key(key).bucket(bucketName)))
                .doOnNext(deleteObjectTaggingResponse ->  log.info(CLIENT_METHOD_RETURN, DELETE_OBJECT_TAGGING, deleteObjectTaggingResponse))
                .retryWhen(s3RetryStrategy);
    }

}
