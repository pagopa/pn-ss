package it.pagopa.pnss.transformation.service;

import reactor.core.publisher.Mono;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.services.s3.model.DeleteObjectResponse;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

public interface S3Service {

    Mono<ResponseBytes<GetObjectResponse>> getObject(String key, String bucketName);
    Mono<PutObjectResponse> putObject(String key, byte[] fileBytes, String bucketName);
    Mono<DeleteObjectResponse> deleteObject(String key, String bucketName);
}
