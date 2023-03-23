package it.pagopa.pnss.transformation.service;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import it.pagopa.pnss.configurationproperties.BucketName;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

@Service
@Slf4j
public class UploadObjectService extends CommonS3ObjectService {

	@Autowired
	private BucketName bucketName;

	public Mono<PutObjectResponse> execute(String documentKey, byte[] fileSigned) {
		log.info("UploadObjectService.execute() : START");
		log.debug("UploadObjectService.execute() : documentKey {} : documentState {} : documentType {}", documentKey);

		S3AsyncClient s3 = getS3AsynchClient();

		return Mono.just(PutObjectRequest.builder()
						         .bucket(bucketName.ssHotName())
						         .contentMD5(new String(Base64.encodeBase64(DigestUtils.md5(fileSigned))))
						         .key(documentKey)
						         .build())
			.flatMap(putObjectRequest -> Mono.fromCompletionStage(s3.putObject(putObjectRequest, AsyncRequestBody.fromBytes(fileSigned))));

//		return retentionService
//				.getPutObjectRequestForObjectInBucket(bucketName.ssHotName(), fileSigned, documentKey, documentState,
//						documentType.getTipoDocumento())
//				.flatMap(objectRequest -> Mono
//						.fromCompletionStage(s3.putObject(objectRequest, AsyncRequestBody.fromBytes(fileSigned))));

	}
}
