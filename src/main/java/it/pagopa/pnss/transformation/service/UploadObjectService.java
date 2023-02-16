package it.pagopa.pnss.transformation.service;

import it.pagopa.pnss.configurationproperties.BucketName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

import java.nio.ByteBuffer;

import static it.pagopa.pnss.common.Constant.*;

@Service
public class UploadObjectService  extends  CommonS3ObjectService {


//    @Value("${S3.bucket.hot.name}")
//    public  String bucketHot;

    @Autowired
    private BucketName bucketName;
    public PutObjectResponse execute(String key, byte[] fileSigned){
        S3Client s3 = getS3Client();
        PutObjectRequest objectRequest = PutObjectRequest.builder()
                .bucket(bucketName.ssHotName())
                .key(key)
                //.tagging(STORAGETYPE+ PN_LEGAL_FACTS) //Da verificare storageType
                .build();

        PutObjectResponse putObjectResponse = s3.putObject(objectRequest, RequestBody.fromBytes(fileSigned));
        return putObjectResponse;
    }
}
