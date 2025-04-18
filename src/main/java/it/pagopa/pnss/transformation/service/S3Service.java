package it.pagopa.pnss.transformation.service;

import reactor.core.publisher.Mono;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.time.Duration;
import java.util.List;

public interface S3Service {

    Mono<ResponseBytes<GetObjectResponse>> getObject(String key, String bucketName);

    Mono<PutObjectResponse> putObject(String key, byte[] fileBytes, String contentType, String bucketName, Tagging tagging);

    Mono<PutObjectResponse> putObject(String key, byte[] fileBytes, String contentType, String bucketName);

    Mono<DeleteObjectResponse> deleteObject(String key, String bucketName);

    Mono<RestoreObjectResponse> restoreObject(String key, String bucketName, RestoreRequest restoreRequest);

    Mono<PresignedGetObjectRequest> presignGetObject(GetObjectRequest getObjectRequest, Duration duration);

    Mono<GetBucketLifecycleConfigurationResponse> getBucketLifecycleConfiguration(String bucket);

    Mono<HeadObjectResponse> headObject(String key, String bucket);

    Mono<PutObjectRetentionResponse> putObjectRetention(String key, String bucket, ObjectLockRetention objectLockRetention);

    Mono<PutObjectTaggingResponse> putObjectTagging(String key, String bucket, Tagging tagging);

    Mono<GetObjectTaggingResponse> getObjectTagging(String key, String bucket);

    Mono<ListObjectVersionsResponse> listObjectVersions(String key, String bucket);

    Mono<DeleteObjectsResponse> deleteObjectVersions(String key, String bucketName, List<ObjectIdentifier> identifiers);

}
