package it.pagopa.pnss.transformation.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

@Service
public class DownloadObjectService extends  CommonS3ObjectService {

    @Value("${S3.bucket.stage.name}")
    public  String bucketStage;

    public ResponseBytes<GetObjectResponse> execute(String key){
        S3Client s3 = getS3Client();
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketStage)
                .key(key)
                .build();


        ResponseBytes<GetObjectResponse> object = s3.getObject(getObjectRequest, ResponseTransformer.toBytes());

        return object!=null ? object : null ;
    }

}
