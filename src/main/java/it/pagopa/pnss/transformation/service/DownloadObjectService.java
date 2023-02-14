package it.pagopa.pnss.transformation.service;

import it.pagopa.pnss.configurationproperties.BucketName;
import org.springframework.beans.factory.annotation.Autowired;
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

    @Autowired
    private BucketName bucketName;

    public ResponseBytes<GetObjectResponse> execute(String key){
        S3Client s3 = getS3Client();
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName.ssStageName())
                .key(key)
                .build();


        ResponseBytes<GetObjectResponse> object = s3.getObject(getObjectRequest, ResponseTransformer.toBytes());

        return object!=null ? object : null ;
    }

}
