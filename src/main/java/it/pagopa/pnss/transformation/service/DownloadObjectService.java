package it.pagopa.pnss.transformation.service;

import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

@Service
@Slf4j
public class DownloadObjectService extends  CommonS3ObjectService {

    public ResponseBytes<GetObjectResponse> execute(String key, String bucketNameFromS3){
    	log.info("DownloadObjectService.execute() : START");
    	log.debug("DownloadObjectService.execute() : key = {}, bucketNameFromS3 = {}", key, bucketNameFromS3);
    	
        S3Client s3 = getS3Client();

        String bucket =bucketNameFromS3;
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build();

        ResponseBytes<GetObjectResponse> object = s3.getObject(getObjectRequest, ResponseTransformer.toBytes());

        return object!=null ? object : null ;
    }

}
