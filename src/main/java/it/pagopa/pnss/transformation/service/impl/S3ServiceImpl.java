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
import software.amazon.awssdk.services.s3.model.DeleteObjectResponse;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

import java.util.Arrays;

@Service
@Slf4j
public class S3ServiceImpl implements S3Service {

    private final S3AsyncClient s3AsyncClient;

    public S3ServiceImpl(S3AsyncClient s3AsyncClient) {
        this.s3AsyncClient = s3AsyncClient;
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
        return Mono.fromCallable(() -> Arrays.toString(Base64.encodeBase64(DigestUtils.md5(fileBytes))))
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
                   .doOnNext(putObjectResponse -> log.debug("Delete an object from S3 from bucket {} having key {}", bucketName, key));
    }
}
