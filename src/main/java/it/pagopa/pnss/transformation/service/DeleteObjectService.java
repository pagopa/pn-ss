package it.pagopa.pnss.transformation.service;

import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectResponse;

@Service
@Slf4j
public class DeleteObjectService extends  CommonS3ObjectService {
    
    public Mono<DeleteObjectResponse> execute(String keyName, String bucketNameFromS3){
    	log.info("DeleteObjectService.execute() : START");
    	log.debug("DeleteObjectService.execute() : keyName = {}, bucketNameFromS3 = {}", keyName, bucketNameFromS3);
    	
    	S3AsyncClient s3Client = getS3AsynchClient();
    	String bucket = bucketNameFromS3;
    	
		DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                .bucket(bucket)
                .key(keyName)
                .build();
		return Mono.fromCompletionStage(s3Client.deleteObject(deleteObjectRequest));

    }
}
