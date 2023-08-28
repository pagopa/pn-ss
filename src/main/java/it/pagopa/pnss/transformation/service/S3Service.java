package it.pagopa.pnss.transformation.service;

import reactor.core.publisher.Mono;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.time.Duration;

public interface S3Service {

    Mono<ResponseBytes<GetObjectResponse>> getObject(String key, String bucketName);
    Mono<PutObjectResponse> putObject(String key, byte[] fileBytes, String bucketName);
    Mono<DeleteObjectResponse> deleteObject(String key, String bucketName);

    Mono<RestoreObjectResponse> restoreObject(String key, String bucketName, RestoreRequest restoreRequest);

    Mono<PresignedGetObjectRequest> presignGetObject(GetObjectRequest getObjectRequest, Duration duration);

    Mono<GetBucketLifecycleConfigurationResponse> getBucketLifecycleConfiguration(String bucket);

    Mono<HeadObjectResponse> headObject(String key, String bucket);

    Mono<PutObjectRetentionResponse> putObjectRetention(String key, String bucket, ObjectLockRetention objectLockRetention);
}
