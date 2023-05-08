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
import com.fasterxml.jackson.databind.ObjectMapper;
import io.awspring.cloud.messaging.config.QueueMessageHandlerFactory;
import io.awspring.cloud.messaging.listener.support.AcknowledgmentHandlerMethodArgumentResolver;
import it.pagopa.pnss.availabledocument.event.StreamsRecordProcessorFactory;
import it.pagopa.pnss.configurationproperties.AvailabelDocumentEventBridgeName;
import it.pagopa.pnss.configurationproperties.AwsConfigurationProperties;
import it.pagopa.pnss.configurationproperties.DynamoEventStreamName;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
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
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClientBuilder;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClientBuilder;
import software.amazon.awssdk.services.dynamodb.waiters.DynamoDbAsyncWaiter;
import software.amazon.awssdk.services.dynamodb.waiters.DynamoDbWaiter;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3AsyncClientBuilder;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClientBuilder;
import software.amazon.awssdk.services.sns.SnsAsyncClient;
import software.amazon.awssdk.services.sns.SnsAsyncClientBuilder;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.SqsAsyncClientBuilder;

import java.net.URI;
import java.util.List;

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
    public TaskExecutor taskExecutor() {
        return new SimpleAsyncTaskExecutor(); // Or use another one of your liking
    }

    // TODO: Rifare completamente questa parte riguardante la disponibilità documenti
    @Bean
    public CommandLineRunner schedulingRunner(@Qualifier("taskExecutor") TaskExecutor executor) {
        return args -> {
            AWSCredentialsProvider awsCredentialsProvider = DefaultAWSCredentialsProviderChain.getInstance();
            AmazonDynamoDB amazonDynamoDB =
                    AmazonDynamoDBClientBuilder.standard().withRegion(DEFAULT_AWS_REGION_PROVIDER_CHAIN.getRegion().id()).build();
            AmazonCloudWatch cloudWatchClient =
                    AmazonCloudWatchClientBuilder.standard().withRegion(DEFAULT_AWS_REGION_PROVIDER_CHAIN.getRegion().id()).build();
            AmazonDynamoDBStreams dynamoDBStreamsClient =
                    AmazonDynamoDBStreamsClientBuilder.standard().withRegion(DEFAULT_AWS_REGION_PROVIDER_CHAIN.getRegion().id()).build();
            AmazonDynamoDBStreamsAdapterClient adapterClient = new AmazonDynamoDBStreamsAdapterClient(dynamoDBStreamsClient);
            KinesisClientLibConfiguration workerConfig = new KinesisClientLibConfiguration(dynamoEventStreamName.tableMetadata(),
                                                                                           dynamoEventStreamName.documentName(),
                                                                                           awsCredentialsProvider,
                                                                                           "streams-demo-worker").withMaxLeaseRenewalThreads(
                                                                                                                         5000)

                                                                                                                 .withMaxLeasesForWorker(
                                                                                                                         5000)

//                                                                                                               Fix temporanea per non
//                                                                                                               fare andare in errore
//                                                                                                               EventBridge.
//                                                                                                               Questo stream Kinesis,
//                                                                                                               agganciato a
//                                                                                                               DynamoDbStreams,
//                                                                                                               notifica a EventBridge
//                                                                                                               determinati eventi
//                                                                                                               provenienti dalla
//                                                                                                               tabella documenti. Dato
//                                                                                                               che la pubblicazione su
//                                                                                                               EventBridge accetta massimo
//                                                                                                               10 elementi, il numero
//                                                                                                               di eventi Kinesis è
//                                                                                                               impostato anch'esso a 10
                                                                                                                 .withMaxRecords(10)

                                                                                                                 .withIdleTimeBetweenReadsInMillis(
                                                                                                                         50)
                                                                                                                 .withInitialPositionInStream(
                                                                                                                         InitialPositionInStream.TRIM_HORIZON);

            IRecordProcessorFactory recordProcessorFactory =
                    new StreamsRecordProcessorFactory(availabelDocumentEventBridgeName.disponibilitaDocumentiName());
            Worker worker = StreamsWorkerFactory.createDynamoDbStreamsWorker(recordProcessorFactory,
                                                                             workerConfig,
                                                                             adapterClient,
                                                                             amazonDynamoDB,
                                                                             cloudWatchClient);
            if (testEventBridge == null) {
                //executor.execute(worker);
            }
        };
    }
}
