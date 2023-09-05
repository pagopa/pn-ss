package it.pagopa.pnss.transformation.service.impl;

import it.pagopa.pnss.transformation.service.S3Service;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.time.Duration;

@Service
@Slf4j
public class S3ServiceImpl implements S3Service {

    private final S3AsyncClient s3AsyncClient;

    private final S3Presigner s3Presigner;

    public S3ServiceImpl(S3AsyncClient s3AsyncClient, S3Presigner s3Presigner) {
        this.s3AsyncClient = s3AsyncClient;
        this.s3Presigner = s3Presigner;
    }

    @Override
    public Mono<ResponseBytes<GetObjectResponse>> getObject(String key, String bucketName) {
        return Mono.fromCompletionStage(s3AsyncClient.getObject(builder -> builder.key(key).bucket(bucketName),
                                                                AsyncResponseTransformer.toBytes()))
                   .doOnNext(getObjectResponseResponseBytes -> log.debug("Retrieved an object from S3 from bucket {} having key {}",
                                                                         bucketName,
                                                                         key));
    }

    @Override
    public Mono<PutObjectResponse> putObject(String key, byte[] fileBytes, String bucketName) {
        return Mono.fromCallable(() -> new String(Base64.encodeBase64(DigestUtils.md5(fileBytes))))
                   .doOnNext(md5 -> log.debug("MD5 for key {} -> {}", key, md5))
                   .flatMap(contentMD5 -> Mono.fromCompletionStage(s3AsyncClient.putObject(builder -> builder.key(key)
                                                                                                             .contentMD5(contentMD5)
                                                                                                             .bucket(bucketName),
                                                                                           AsyncRequestBody.fromBytes(fileBytes))))
                   .doOnNext(putObjectResponse -> log.debug("Put an object in S3 in bucket {} having key {}", bucketName, key));

    }

    @Override
    public Mono<DeleteObjectResponse> deleteObject(String key, String bucketName) {
        return Mono.fromCompletionStage(s3AsyncClient.deleteObject(builder -> builder.key(key).bucket(bucketName)))
                   .doOnNext(deleteObjectResponse -> log.debug("Delete an object from S3 from bucket {} having key {}", bucketName, key));
    }

    @Override
    public Mono<RestoreObjectResponse> restoreObject(String key, String bucketName, RestoreRequest restoreRequest) {
        return Mono.fromCompletionStage(s3AsyncClient.restoreObject(builder -> builder.key(key).bucket(bucketName).restoreRequest(restoreRequest)))
                .doOnNext(restoreObjectResponse -> log.debug("Restored an object from S3 from bucket {} having key {}", bucketName, key));
    }

    @Override
    public Mono<PresignedGetObjectRequest> presignGetObject(GetObjectRequest getObjectRequest, Duration duration) {
        return Mono.just(s3Presigner.presignGetObject(builder -> builder.getObjectRequest(getObjectRequest).signatureDuration(duration)))
                .doOnNext(presignedGetObjectRequest -> log.debug("Presigned an S3 object with duration {}", duration));
    }

    @Override
    public Mono<GetBucketLifecycleConfigurationResponse> getBucketLifecycleConfiguration(String bucketName) {
            return Mono.fromCompletionStage(s3AsyncClient.getBucketLifecycleConfiguration(builder -> builder.bucket(bucketName)))
                    .doOnNext(getBucketLifecycleConfigurationResponse -> log.debug("Getting lifecycle configuration from bucket {}", bucketName));

    }

    @Override
    public Mono<HeadObjectResponse> headObject(String key, String bucketName) {
        return Mono.fromCompletionStage(s3AsyncClient.headObject(builder -> builder.key(key).bucket(bucketName).checksumMode(ChecksumMode.ENABLED)))
                .doOnNext(headObjectResponse -> log.debug("Head object in bucket {} having key {}", bucketName, key));
    }

    @Override
    public Mono<PutObjectRetentionResponse> putObjectRetention(String key, String bucketName, ObjectLockRetention objectLockRetention) {
        return Mono.fromCompletionStage(s3AsyncClient.putObjectRetention(builder -> builder.key(key).bucket(bucketName).retention(objectLockRetention)))
                .doOnNext(putObjectRetentionResponse -> log.debug("Put retention to object in bucket {} having key {}", bucketName, key));
    }

}
