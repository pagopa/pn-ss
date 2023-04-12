package it.pagopa.pnss.common.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.awspring.cloud.messaging.config.QueueMessageHandlerFactory;
import io.awspring.cloud.messaging.listener.support.AcknowledgmentHandlerMethodArgumentResolver;
import it.pagopa.pnss.common.configurationproperties.AwsConfigurationProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.handler.annotation.support.PayloadMethodArgumentResolver;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.regions.providers.DefaultAwsRegionProviderChain;
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient;
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClientBuilder;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClientBuilder;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClientBuilder;
import software.amazon.awssdk.services.dynamodb.streams.DynamoDbStreamsAsyncClient;
import software.amazon.awssdk.services.dynamodb.streams.DynamoDbStreamsAsyncClientBuilder;
import software.amazon.awssdk.services.dynamodb.waiters.DynamoDbAsyncWaiter;
import software.amazon.awssdk.services.dynamodb.waiters.DynamoDbWaiter;
import software.amazon.awssdk.services.kinesis.KinesisAsyncClient;
import software.amazon.awssdk.services.kinesis.KinesisAsyncClientBuilder;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3AsyncClientBuilder;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClientBuilder;
import software.amazon.awssdk.services.sns.SnsAsyncClient;
import software.amazon.awssdk.services.sns.SnsAsyncClientBuilder;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.SqsAsyncClientBuilder;
import software.amazon.kinesis.common.KinesisClientUtil;

import java.net.URI;
import java.util.List;

@Configuration
public class AwsConfiguration {

    private final AwsConfigurationProperties awsConfigurationProperties;

    /*
     * Set in LocalStackTestConfig
     */

    @Value("${test.aws.sqs.endpoint:#{null}}")
    String sqsLocalStackEndpoint;

    @Value("${test.aws.dynamodb.endpoint:#{null}}")
    String dynamoDbLocalStackEndpoint;

    @Value("${test.aws.sns.endpoint:#{null}}")
    String snsLocalStackEndpoint;

    @Value("${test.aws.secretsmanager.endpoint:#{null}}")
    String secretsManagerLocalStackEndpoint;

    @Value("${test.aws.s3.endpoint:#{null}}")
    private String s3LocalStackEndpoint;

    @Value("${test.aws.kinesis.endpoint:#{null}}")
    private String kinesisLocalstackEndpoint;

    @Value("${test.aws.cloudwatch.endpoint:#{null}}")
    private String cloudWatchLocalstackEndpoint;

    @Value("${test.aws.dynamodbstreams.endpoint:#{null}}")
    private String dynamoDbStreamsLocalstackEndpoint;

    private static final DefaultAwsRegionProviderChain DEFAULT_AWS_REGION_PROVIDER_CHAIN = new DefaultAwsRegionProviderChain();
    private static final DefaultCredentialsProvider DEFAULT_CREDENTIALS_PROVIDER = DefaultCredentialsProvider.create();

    public AwsConfiguration(AwsConfigurationProperties awsConfigurationProperties) {
        this.awsConfigurationProperties = awsConfigurationProperties;
    }

    //  <-- spring-cloud-starter-aws-messaging -->

    @Bean
    public QueueMessageHandlerFactory queueMessageHandlerFactory(ObjectMapper objectMapper, LocalValidatorFactoryBean validator) {

        final var queueMessageHandlerFactory = new QueueMessageHandlerFactory();
        final var converter = new MappingJackson2MessageConverter();

        converter.setObjectMapper(objectMapper);
        converter.setStrictContentTypeMatch(false);

        final var acknowledgmentResolver = new AcknowledgmentHandlerMethodArgumentResolver("Acknowledgment");

        queueMessageHandlerFactory.setArgumentResolvers(List.of(acknowledgmentResolver,
                                                                new PayloadMethodArgumentResolver(converter, validator)));

        return queueMessageHandlerFactory;
    }

//  <-- AWS SDK for Java v2 -->

    @Bean
    public SqsAsyncClient sqsAsyncClient() {
        SqsAsyncClientBuilder sqsAsyncClientBuilder = SqsAsyncClient.builder()
                                                                    .credentialsProvider(DEFAULT_CREDENTIALS_PROVIDER)
                                                                    .region(Region.of(awsConfigurationProperties.regionCode()));

        if (sqsLocalStackEndpoint != null) {
            sqsAsyncClientBuilder.endpointOverride(URI.create(sqsLocalStackEndpoint));
        }

        return sqsAsyncClientBuilder.build();
    }

    @Bean
    public DynamoDbClient dynamoDbClient() {
        DynamoDbClientBuilder dynamoDbClientBuilder = DynamoDbClient.builder()
                                                                    .credentialsProvider(DEFAULT_CREDENTIALS_PROVIDER)
                                                                    .region(Region.of(awsConfigurationProperties.regionCode()));

        if (dynamoDbLocalStackEndpoint != null) {
            dynamoDbClientBuilder.endpointOverride(URI.create(dynamoDbLocalStackEndpoint));
        }

        return dynamoDbClientBuilder.build();
    }

    @Bean
    public DynamoDbEnhancedClient dynamoDbEnhancedClient(DynamoDbClient dynamoDbClient) {
        return DynamoDbEnhancedClient.builder().dynamoDbClient(dynamoDbClient).build();
    }

    @Bean
    public DynamoDbWaiter dynamoDbWaiter(DynamoDbClient dynamoDbClient) {
        return DynamoDbWaiter.builder().client(dynamoDbClient).build();
    }

    @Bean
    public DynamoDbAsyncClient dynamoDbAsyncClient() {
        DynamoDbAsyncClientBuilder dynamoDbAsyncClientBuilder = DynamoDbAsyncClient.builder()
                                                                                   .credentialsProvider(DEFAULT_CREDENTIALS_PROVIDER)
                                                                                   .region(Region.of(awsConfigurationProperties.regionCode()));

        if (dynamoDbLocalStackEndpoint != null) {
            dynamoDbAsyncClientBuilder.endpointOverride(URI.create(dynamoDbLocalStackEndpoint));
        }

        return dynamoDbAsyncClientBuilder.build();
    }

    @Bean
    public DynamoDbEnhancedAsyncClient dynamoDbEnhancedAsyncClient(DynamoDbAsyncClient dynamoDbAsyncClient) {
        return DynamoDbEnhancedAsyncClient.builder().dynamoDbClient(dynamoDbAsyncClient).build();
    }

    @Bean
    public DynamoDbAsyncWaiter dynamoDbAsyncWaiter(DynamoDbAsyncClient dynamoDbAsyncClient) {
        return DynamoDbAsyncWaiter.builder().client(dynamoDbAsyncClient).build();
    }

    @Bean
    public SnsAsyncClient snsClient() {
        SnsAsyncClientBuilder snsAsyncClientBuilder = SnsAsyncClient.builder()
                                                                    .credentialsProvider(DEFAULT_CREDENTIALS_PROVIDER)
                                                                    .region(Region.of(awsConfigurationProperties.regionCode()));

        if (snsLocalStackEndpoint != null) {
            snsAsyncClientBuilder.endpointOverride(URI.create(snsLocalStackEndpoint));
        }

        return snsAsyncClientBuilder.build();
    }

    @Bean
    public S3AsyncClient s3AsyncClient() {
        S3AsyncClientBuilder s3Client = S3AsyncClient.builder()
                                                     .credentialsProvider(DEFAULT_CREDENTIALS_PROVIDER)
                                                     .region(Region.of(awsConfigurationProperties.regionCode()));

        if (s3LocalStackEndpoint != null) {
            s3Client.endpointOverride(URI.create(s3LocalStackEndpoint));
        }

        return s3Client.build();
    }

    @Bean
    public SecretsManagerClient secretsManagerClient() {
        SecretsManagerClientBuilder secretsManagerClient = SecretsManagerClient.builder()
                                                                               .credentialsProvider(DEFAULT_CREDENTIALS_PROVIDER)
                                                                               .region(Region.of(awsConfigurationProperties.regionCode()));

        if (secretsManagerLocalStackEndpoint != null) {
            secretsManagerClient.endpointOverride(URI.create(secretsManagerLocalStackEndpoint));
        }

        return secretsManagerClient.build();
    }

    @Bean
    public KinesisAsyncClient kinesisAsyncClient() {
        KinesisAsyncClientBuilder kinesisAsyncClientBuilder = KinesisAsyncClient.builder()
                                                                                .region(DEFAULT_AWS_REGION_PROVIDER_CHAIN.getRegion())
                                                                                .credentialsProvider(DEFAULT_CREDENTIALS_PROVIDER);

        if (kinesisLocalstackEndpoint != null) {
            kinesisAsyncClientBuilder.endpointOverride(URI.create(kinesisLocalstackEndpoint));
        }

        return KinesisClientUtil.createKinesisAsyncClient(kinesisAsyncClientBuilder);
    }

    @Bean
    public CloudWatchAsyncClient cloudWatchAsyncClient() {
        CloudWatchAsyncClientBuilder cloudWatchAsyncClientBuilder = CloudWatchAsyncClient.builder()
                                                                                         .region(DEFAULT_AWS_REGION_PROVIDER_CHAIN.getRegion())
                                                                                         .credentialsProvider(DEFAULT_CREDENTIALS_PROVIDER);

        if (cloudWatchLocalstackEndpoint != null) {
            cloudWatchAsyncClientBuilder.endpointOverride(URI.create(cloudWatchLocalstackEndpoint));
        }

        return cloudWatchAsyncClientBuilder.build();
    }

    @Bean
    public DynamoDbStreamsAsyncClient dynamoDbStreamsAsyncClient() {
        DynamoDbStreamsAsyncClientBuilder dynamoDbStreamsAsyncClientBuilder = DynamoDbStreamsAsyncClient.builder()
                                                                                         .region(DEFAULT_AWS_REGION_PROVIDER_CHAIN.getRegion())
                                                                                         .credentialsProvider(DEFAULT_CREDENTIALS_PROVIDER);

        if (dynamoDbStreamsLocalstackEndpoint != null) {
            dynamoDbStreamsAsyncClientBuilder.endpointOverride(URI.create(dynamoDbStreamsLocalstackEndpoint));
        }

        return dynamoDbStreamsAsyncClientBuilder.build();
    }

    @Bean
    public TaskExecutor taskExecutor() {
        return new SimpleAsyncTaskExecutor();
    }
}
