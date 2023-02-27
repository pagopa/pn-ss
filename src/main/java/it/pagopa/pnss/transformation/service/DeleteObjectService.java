package it.pagopa.pnss.transformation.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import it.pagopa.pnss.configurationproperties.BucketName;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectResponse;

@Service
public class DeleteObjectService extends  CommonS3ObjectService {


    @Autowired
    private BucketName bucketName;
    
    public Mono<DeleteObjectResponse> execute(String keyName, String bucketNameFromS3){
    	
    	S3AsyncClient s3Client = getS3AsynchClient();
    	String bucket = bucketNameFromS3;
    	
		DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                .bucket(bucket)
                .key(keyName)
                .build();
		return Mono.fromCompletionStage(s3Client.deleteObject(deleteObjectRequest));

    }
}
