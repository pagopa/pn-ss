package it.pagopa.pnss.configuration;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.awspring.cloud.messaging.config.QueueMessageHandlerFactory;
import io.awspring.cloud.messaging.listener.support.AcknowledgmentHandlerMethodArgumentResolver;
import it.pagopa.pnss.configurationproperties.AvailabelDocumentEventBridgeName;
import it.pagopa.pnss.configurationproperties.AwsConfigurationProperties;
import it.pagopa.pnss.configurationproperties.DynamoEventStreamName;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
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
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClientBuilder;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClientBuilder;
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
import software.amazon.kinesis.common.ConfigsBuilder;
import software.amazon.kinesis.common.KinesisClientUtil;
import software.amazon.kinesis.common.StreamIdentifier;
import software.amazon.kinesis.coordinator.Scheduler;
import software.amazon.kinesis.processor.ShardRecordProcessor;
import software.amazon.kinesis.processor.ShardRecordProcessorFactory;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@Configuration
public class AwsConfiguration {

    private final DynamoEventStreamName dynamoEventStreamName;
    private final AvailabelDocumentEventBridgeName availabelDocumentEventBridgeName;
    private final AwsConfigurationProperties awsConfigurationProperties;

    /*
     * Set in LocalStackTestConfig
     */

    @Value("${test.aws.region:#{null}}")
    String localStackRegion;

    @Value("${test.aws.sqs.endpoint:#{null}}")
    String sqsLocalStackEndpoint;

    @Value("${test.aws.dynamodb.endpoint:#{null}}")
    String dynamoDbLocalStackEndpoint;

    @Value("${test.aws.sns.endpoint:#{null}}")
    String snsLocalStackEndpoint;

    @Value("${test.aws.secretsmanager.endpoint:#{null}}")
    String secretsManagerLocalStackEndpoint;

    @Value("${test.event.bridge:#{null}}")
    private String testEventBridge;

    @Value("${test.aws.s3.endpoint:#{null}}")
    private String testAwsS3Endpoint;

    @Value("${test.aws.kinesis.endpoint:#{null}}")
    private String testKinesisEndPoint;

    @Value("${test.aws.cloudwatch.endpoint:#{null}}")
    private String testCloudWatchEndPoint;

    private static final DefaultAwsRegionProviderChain DEFAULT_AWS_REGION_PROVIDER_CHAIN = new DefaultAwsRegionProviderChain();
    private static final DefaultCredentialsProvider DEFAULT_CREDENTIALS_PROVIDER = DefaultCredentialsProvider.create();

    public AwsConfiguration(AwsConfigurationProperties awsConfigurationProperties, DynamoEventStreamName dynamoEventStreamName,
                            AvailabelDocumentEventBridgeName availabelDocumentEventBridgeName) {
        this.awsConfigurationProperties = awsConfigurationProperties;
        this.dynamoEventStreamName = dynamoEventStreamName;
        this.availabelDocumentEventBridgeName = availabelDocumentEventBridgeName;
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
        SqsAsyncClientBuilder sqsAsyncClientBuilder = SqsAsyncClient.builder().credentialsProvider(DEFAULT_CREDENTIALS_PROVIDER);

        if (sqsLocalStackEndpoint != null) {
            sqsAsyncClientBuilder.region(Region.of(localStackRegion)).endpointOverride(URI.create(sqsLocalStackEndpoint));
        } else {
            sqsAsyncClientBuilder.region(DEFAULT_AWS_REGION_PROVIDER_CHAIN.getRegion());
        }

        return sqsAsyncClientBuilder.build();
    }

    @Bean
    public DynamoDbClient dynamoDbClient() {
        DynamoDbClientBuilder dynamoDbClientBuilder = DynamoDbClient.builder().credentialsProvider(DEFAULT_CREDENTIALS_PROVIDER);

        if (dynamoDbLocalStackEndpoint != null) {
            dynamoDbClientBuilder.region(Region.of(localStackRegion)).endpointOverride(URI.create(dynamoDbLocalStackEndpoint));
        } else {
            dynamoDbClientBuilder.region(DEFAULT_AWS_REGION_PROVIDER_CHAIN.getRegion());
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
        DynamoDbAsyncClientBuilder dynamoDbAsyncClientBuilder =
                DynamoDbAsyncClient.builder().credentialsProvider(DEFAULT_CREDENTIALS_PROVIDER);

        if (dynamoDbLocalStackEndpoint != null) {
            dynamoDbAsyncClientBuilder.region(Region.of(localStackRegion)).endpointOverride(URI.create(dynamoDbLocalStackEndpoint));
        } else {
            dynamoDbAsyncClientBuilder.region(DEFAULT_AWS_REGION_PROVIDER_CHAIN.getRegion());
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
        SnsAsyncClientBuilder snsAsyncClientBuilder = SnsAsyncClient.builder().credentialsProvider(DEFAULT_CREDENTIALS_PROVIDER);

        if (snsLocalStackEndpoint != null) {
            snsAsyncClientBuilder.region(Region.of(localStackRegion)).endpointOverride(URI.create(snsLocalStackEndpoint));
        } else {
            snsAsyncClientBuilder.region(DEFAULT_AWS_REGION_PROVIDER_CHAIN.getRegion());
        }

        return snsAsyncClientBuilder.build();
    }

    @Bean
    public S3AsyncClient s3AsyncClient() {
        S3AsyncClientBuilder s3Client = S3AsyncClient.builder()
                                                     .credentialsProvider(DEFAULT_CREDENTIALS_PROVIDER)
                                                     .region(Region.of(awsConfigurationProperties.regionCode()));

        if (testAwsS3Endpoint != null) {
            s3Client.endpointOverride(URI.create(testAwsS3Endpoint));
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
    public KinesisAsyncClient kinesisAsyncClient(){
        KinesisAsyncClientBuilder kinesisAsyncClientBuilder =
                KinesisAsyncClient.builder().region(DEFAULT_AWS_REGION_PROVIDER_CHAIN.getRegion()).credentialsProvider(DEFAULT_CREDENTIALS_PROVIDER);

        if(testKinesisEndPoint != null){
            kinesisAsyncClientBuilder.endpointOverride(URI.create(testKinesisEndPoint));
        }
        return KinesisClientUtil.createKinesisAsyncClient(kinesisAsyncClientBuilder);
    }


    @Bean
    public TaskExecutor taskExecutor() {
        return new SimpleAsyncTaskExecutor(); // Or use another one of your liking
    }

    @Bean
    public CommandLineRunner schedulingRunner(@Qualifier("taskExecutor") TaskExecutor executor, DynamoDbAsyncClient dynamoDbAsyncClient, KinesisAsyncClient kinesisAsyncClient) {
        return args -> {

            CloudWatchAsyncClient cloudWatchClient = CloudWatchAsyncClient.builder()
                                                            .region(DEFAULT_AWS_REGION_PROVIDER_CHAIN.getRegion())
                                                            .credentialsProvider(DEFAULT_CREDENTIALS_PROVIDER).build();
            ConfigsBuilder configsBuilder = new ConfigsBuilder(dynamoEventStreamName.tableMetadata(), dynamoEventStreamName.documentName(), kinesisAsyncClient, dynamoDbAsyncClient, cloudWatchClient, UUID.randomUUID().toString(), new SampleRecordProcessorFactory(availabelDocumentEventBridgeName.disponibilitaDocumentiName()));

            Scheduler scheduler = new Scheduler(
                    configsBuilder.checkpointConfig(),
                    configsBuilder.coordinatorConfig(),
                    configsBuilder.leaseManagementConfig(),
                    configsBuilder.lifecycleConfig(),
                    configsBuilder.metricsConfig(),
                    configsBuilder.processorConfig(),
                    configsBuilder.retrievalConfig()
            );

            if (testEventBridge == null) {
                executor.execute(scheduler);
            }
        };
    }
}
