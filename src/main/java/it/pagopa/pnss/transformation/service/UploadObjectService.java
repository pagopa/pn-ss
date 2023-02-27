package it.pagopa.pnss.transformation.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import it.pagopa.pn.template.internal.rest.v1.dto.DocumentType;
import it.pagopa.pnss.common.retention.RetentionService;
import it.pagopa.pnss.configurationproperties.BucketName;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

@Service
@Slf4j
public class UploadObjectService  extends  CommonS3ObjectService {
	
	private final RetentionService retentionService;
	
	public UploadObjectService(RetentionService retentionService) {
		this.retentionService = retentionService;
	}
	
//    @Value("${S3.bucket.hot.name}")
//    public  String bucketHot;

    @Autowired
    private BucketName bucketName;
    public Mono<PutObjectResponse> execute(String key, byte[] fileSigned, String documentState, DocumentType documentType){
    	S3AsyncClient s3 = getS3AsynchClient();
    	
        log.debug("UploadObjectService.execute() : put ObjectLockConfiguration PRE");
        return retentionService.getPutObjectRequestForObjectInBucket(bucketName.ssHotName(), fileSigned, key, documentState, documentType.getTipoDocumento())
        	.flatMap(objectRequest -> Mono.fromCompletionStage(s3.putObject(objectRequest, AsyncRequestBody.fromBytes(fileSigned)))); 
        
//        PutObjectResponse putObjectResponse = s3.putObject(objectRequest, RequestBody.fromBytes(fileSigned));
//        return putObjectResponse;
    }
}
