package it.pagopa.pnss.configuration;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClientBuilder;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBStreams;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBStreamsClientBuilder;
import com.amazonaws.services.dynamodbv2.streamsadapter.AmazonDynamoDBStreamsAdapterClient;
import com.amazonaws.services.dynamodbv2.streamsadapter.StreamsWorkerFactory;
import com.amazonaws.services.kinesis.clientlibrary.interfaces.v2.IRecordProcessorFactory;
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.InitialPositionInStream;
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.KinesisClientLibConfiguration;
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.Worker;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.awspring.cloud.messaging.config.QueueMessageHandlerFactory;
import io.awspring.cloud.messaging.listener.support.AcknowledgmentHandlerMethodArgumentResolver;
import it.pagopa.pnss.configurationproperties.AvailabelDocumentEventBridgeName;
import it.pagopa.pnss.configurationproperties.AwsConfigurationProperties;
import it.pagopa.pnss.configurationproperties.DynamoEventStreamName;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.handler.annotation.support.PayloadMethodArgumentResolver;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
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
import software.amazon.awssdk.services.dynamodb.waiters.DynamoDbAsyncWaiter;
import software.amazon.awssdk.services.dynamodb.waiters.DynamoDbWaiter;
import software.amazon.awssdk.services.eventbridge.EventBridgeAsyncClient;
import software.amazon.awssdk.services.eventbridge.EventBridgeAsyncClientBuilder;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3AsyncClientBuilder;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.sns.SnsAsyncClient;
import software.amazon.awssdk.services.sns.SnsAsyncClientBuilder;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.SqsAsyncClientBuilder;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.SsmClientBuilder;

import software.amazon.awssdk.services.ssm.SsmAsyncClient;
import software.amazon.awssdk.services.ssm.SsmAsyncClientBuilder;
import java.net.URI;
import java.util.List;

@Configuration
@Slf4j
public class AwsConfiguration {

    private final DynamoEventStreamName dynamoEventStreamName;
    private final AvailabelDocumentEventBridgeName availabelDocumentEventBridgeName;
    private final AwsConfigurationProperties awsConfigurationProperties;
    private final WebClient genericWebClient = WebClient.builder().build();

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

    @Value("${test.aws.eventbridge.endpoint:#{null}}")
    String eventBridgeLocalStackEndpoint;

    @Value("${test.event.bridge:#{null}}")
    private String testEventBridge;

    @Value("${test.aws.s3.endpoint:#{null}}")
    private String testAwsS3Endpoint;

    @Value("${test.aws.cloudwatch.endpoint:#{null}}")
    private String testAwsCloudwatchEndpoint;

    @Value("${test.aws.ssm.endpoint:#{null}}")
    private String testAwsSsmEndpoint;

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
        SqsAsyncClientBuilder sqsAsyncClientBuilder = SqsAsyncClient.builder().credentialsProvider(DEFAULT_CREDENTIALS_PROVIDER).region(Region.of(awsConfigurationProperties.regionCode()));

        if (sqsLocalStackEndpoint != null) {
            sqsAsyncClientBuilder.endpointOverride(URI.create(sqsLocalStackEndpoint));
        }

        return sqsAsyncClientBuilder.build();
    }

    @Bean
    public DynamoDbClient dynamoDbClient() {
        DynamoDbClientBuilder dynamoDbClientBuilder = DynamoDbClient.builder().credentialsProvider(DEFAULT_CREDENTIALS_PROVIDER).region(Region.of(awsConfigurationProperties.regionCode()));

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
        DynamoDbAsyncClientBuilder dynamoDbAsyncClientBuilder =
                DynamoDbAsyncClient.builder().credentialsProvider(DEFAULT_CREDENTIALS_PROVIDER).region(Region.of(awsConfigurationProperties.regionCode()));

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
        SnsAsyncClientBuilder snsAsyncClientBuilder = SnsAsyncClient.builder().credentialsProvider(DEFAULT_CREDENTIALS_PROVIDER).region(Region.of(awsConfigurationProperties.regionCode()));

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

        if (testAwsS3Endpoint != null) {
            s3Client.endpointOverride(URI.create(testAwsS3Endpoint));
        }

        return s3Client.build();
    }

    @Bean
    public SsmAsyncClient ssmAsyncClient() {
        SsmAsyncClientBuilder ssmClient = SsmAsyncClient.builder()
                .credentialsProvider(DEFAULT_CREDENTIALS_PROVIDER)
                .region(Region.of(awsConfigurationProperties.regionCode()));

        if (testAwsSsmEndpoint != null) {
            ssmClient.endpointOverride(URI.create(testAwsSsmEndpoint));
        }

        return ssmClient.build();
    }

    @Bean
    public S3Presigner s3Presigner()
    {
        S3Presigner.Builder builder = S3Presigner.builder()
                .credentialsProvider(DEFAULT_CREDENTIALS_PROVIDER)
                .region(Region.of(awsConfigurationProperties.regionCode()));

        if (testAwsS3Endpoint != null) {
            builder.endpointOverride(URI.create(testAwsS3Endpoint));
        }

        return builder.build();
    }

    @Bean
    public CloudWatchAsyncClient cloudWatchAsyncClient() {
        CloudWatchAsyncClientBuilder cloudWatchAsyncClientBuilder = CloudWatchAsyncClient.builder().credentialsProvider(DEFAULT_CREDENTIALS_PROVIDER).region(Region.of(awsConfigurationProperties.regionCode()));

        if (testAwsCloudwatchEndpoint != null) {
            cloudWatchAsyncClientBuilder.endpointOverride(URI.create(testAwsCloudwatchEndpoint));
        }

        return cloudWatchAsyncClientBuilder.build();
    }

    @Bean
    public SsmClient ssmClient() {
        SsmClientBuilder ssmClientBuilder = SsmClient.builder().credentialsProvider(DEFAULT_CREDENTIALS_PROVIDER).region(Region.of(awsConfigurationProperties.regionCode()));

        if (testAwsSsmEndpoint != null) {
            ssmClientBuilder.endpointOverride(URI.create(testAwsSsmEndpoint));
        }

        return ssmClientBuilder.build();
    }

    @Bean
    public EventBridgeAsyncClient eventBridgeAsyncClient() {
        EventBridgeAsyncClientBuilder eventBridgeAsyncClientBuilder = EventBridgeAsyncClient.builder().credentialsProvider(DEFAULT_CREDENTIALS_PROVIDER).region(Region.of(awsConfigurationProperties.regionCode()));

        if (testAwsSsmEndpoint != null) {
            eventBridgeAsyncClientBuilder.endpointOverride(URI.create(eventBridgeLocalStackEndpoint));
        }

        return eventBridgeAsyncClientBuilder.build();
    }

    private String getTaskId() {

        String ecsMetadataUri = System.getenv("ECS_CONTAINER_METADATA_URI_V4");

        if (ecsMetadataUri == null) {
            log.error("ECS_CONTAINER_METADATA_URI_V4 environment variable not found.");
            return "streams-worker";
        }


        return genericWebClient.get()
                .retrieve()
                .bodyToMono(String.class)
                .flatMap(response -> {
                    ObjectMapper objectMapper = new ObjectMapper();
                    try {
                        JsonNode jsonNode = objectMapper.readTree(response);
                        String taskArn = jsonNode.get("TaskARN").asText();
                        String[] parts = taskArn.split("/");
                        if (parts.length > 0) {
                            return Mono.just(parts[parts.length - 1]);
                        } else {
                            log.error("Invalid TaskARN format");
                            return Mono.just("streams-worker");
                        }
                   } catch (JsonProcessingException e) {
                        log.error("Error while parsing JSON response", e);
                        return Mono.just("streams-worker");
                    }
                })
                .onErrorResume(throwable -> {
                    log.error("Error while fetching container metadata", throwable);
                    return Mono.just("streams-worker");
                }).block();
    }
}



