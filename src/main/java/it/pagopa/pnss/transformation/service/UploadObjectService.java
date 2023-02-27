package it.pagopa.pnss.transformation.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import it.pagopa.pn.template.internal.rest.v1.dto.DocumentType;
import it.pagopa.pnss.common.retention.RetentionService;
import it.pagopa.pnss.configurationproperties.BucketName;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
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
    public PutObjectResponse execute(String key, byte[] fileSigned, String documentState, DocumentType documentType){
        S3Client s3 = getS3Client();
        
        log.debug("UploadObjectService.execute() : put ObjectLockConfiguration PRE");
        s3.putObjectLockConfiguration(retentionService.getPutObjectLockConfigurationRequest(key, documentState, documentType));
        log.debug("UploadObjectService.execute() : put ObjectLockConfiguration OK");
        
        PutObjectRequest objectRequest = PutObjectRequest.builder()
                .bucket(bucketName.ssHotName())
                .key(key)
                .build();

        PutObjectResponse putObjectResponse = s3.putObject(objectRequest, RequestBody.fromBytes(fileSigned));
        return putObjectResponse;
    }
}
