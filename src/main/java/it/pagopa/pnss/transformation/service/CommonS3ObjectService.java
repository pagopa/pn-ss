package it.pagopa.pnss.transformation.service;

import java.net.URI;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import it.pagopa.pnss.configurationproperties.AwsConfigurationProperties;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3AsyncClientBuilder;
import software.amazon.awssdk.services.s3.S3Client;

@Service
public abstract class CommonS3ObjectService {
	
	@Value("${test.aws.s3.endpoint:#{null}}")
	private String testAwsS3Endpoint;
	
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
    	S3AsyncClientBuilder s3Client = S3AsyncClient.builder()
    			.region(Region.of(awsConfigurationProperties.regionCode()))
    			.credentialsProvider(ProfileCredentialsProvider.create());
    	if (testAwsS3Endpoint != null) {
    		s3Client.endpointOverride(URI.create(testAwsS3Endpoint));
    	}
    	return s3Client.build();
    }
}
