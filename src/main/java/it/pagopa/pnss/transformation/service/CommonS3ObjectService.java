package it.pagopa.pnss.transformation.service;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import static it.pagopa.pnss.common.Constant.EU_CENTRAL_1;

public class CommonS3ObjectService {


    public S3Client getS3Client(){
        Region region = EU_CENTRAL_1;
        S3Client s3Client = S3Client.builder()
                .region(region)
                .build();
        return s3Client;
    }
}
