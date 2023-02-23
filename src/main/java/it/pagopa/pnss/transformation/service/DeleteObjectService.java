package it.pagopa.pnss.transformation.service;

import it.pagopa.pnss.configurationproperties.BucketName;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

@Service
public class DeleteObjectService extends  CommonS3ObjectService {


    @Autowired
    private BucketName bucketName;
    public DeleteObjectResponse execute(String keyName, String bucketNameFromS3){
       S3Client s3Client = getS3Client();
       String bucket =bucketNameFromS3;

       DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                .bucket(bucket)
                .key(keyName)
                .build();

        return s3Client.deleteObject(deleteObjectRequest);

    }
}
