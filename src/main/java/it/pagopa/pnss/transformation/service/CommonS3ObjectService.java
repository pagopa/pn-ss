package it.pagopa.pnss.transformation.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import it.pagopa.pnss.configurationproperties.AwsConfigurationProperties;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3Client;

@Service
public abstract class CommonS3ObjectService {
	
	@Autowired
	private AwsConfigurationProperties awsConfigurationProperties;

//    public S3Client getS3Client(){
//        Region region = EU_CENTRAL_1;
//        S3Client s3Client = S3Client.builder()
//                .region(region)
//                .build();
//        return s3Client;
//    }
    
    public S3Client getS3Client(){
    	return S3Client.builder()
    			.region(Region.of(awsConfigurationProperties.regionCode()))
    			.build();
    }
    
    public S3AsyncClient getS3AsynchClient() {
    	return S3AsyncClient.builder()
    			.region(Region.of(awsConfigurationProperties.regionCode()))
    			.build();
    }
}
