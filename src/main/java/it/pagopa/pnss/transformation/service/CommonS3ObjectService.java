package it.pagopa.pnss.transformation.service;

import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import it.pagopa.pnss.common.configurationproperties.AwsConfigurationProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3AsyncClientBuilder;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;

@Service
public abstract class CommonS3ObjectService {

    @Value("${test.aws.s3.endpoint:#{null}}")
    private String testAwsS3Endpoint;

    @Autowired
    private AwsConfigurationProperties awsConfigurationProperties;

    private static final DefaultCredentialsProvider DEFAULT_CREDENTIALS_PROVIDER = DefaultCredentialsProvider.create();

    public S3AsyncClient getS3AsyncClient() {
        S3AsyncClientBuilder s3Client = S3AsyncClient.builder()
                                                     .credentialsProvider(DEFAULT_CREDENTIALS_PROVIDER)
                                                     .region(Region.of(awsConfigurationProperties.regionCode()));
        if (testAwsS3Endpoint != null) {
            s3Client.endpointOverride(URI.create(testAwsS3Endpoint));
        }
        return s3Client.build();
    }

    public S3Presigner getS3Presigner() {
        S3Presigner.Builder builder = S3Presigner.builder().region(Region.of(awsConfigurationProperties.regionCode()));
        if (testAwsS3Endpoint != null) {
            builder.endpointOverride(URI.create(testAwsS3Endpoint));
        }

        return builder.build();
    }

    public AmazonS3 getAmazonS3() {
        AmazonS3ClientBuilder builder = AmazonS3ClientBuilder.standard();


        if (testAwsS3Endpoint != null) {
            AwsClientBuilder.EndpointConfiguration endpointConfiguration =
                    new AwsClientBuilder.EndpointConfiguration(testAwsS3Endpoint, awsConfigurationProperties.regionCode());
            builder.setEndpointConfiguration(endpointConfiguration);
        } else {
            builder.withRegion(awsConfigurationProperties.regionCode());
        }
        return builder.build();
    }
}
