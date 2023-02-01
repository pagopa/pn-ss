package it.pagopa.pnss.configuration;

import java.net.URI;
import java.util.Collections;

import com.amazonaws.services.sqs.AmazonSQSAsync;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.awspring.cloud.messaging.config.QueueMessageHandlerFactory;
import io.awspring.cloud.messaging.core.QueueMessagingTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.handler.annotation.support.PayloadMethodArgumentResolver;
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
import software.amazon.awssdk.services.sns.SnsAsyncClient;
import software.amazon.awssdk.services.sns.SnsAsyncClientBuilder;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.SqsAsyncClientBuilder;

@Configuration
public class AwsConfiguration {

    /**
     * Set in LocalStackTestConfig
     */
    @Value("${test.aws.region:#{null}}")
    String localStackRegion;

    /**
     * Set in LocalStackTestConfig
     */
    @Value("${test.aws.sqs.endpoint:#{null}}")
    String sqsLocalStackEndpoint;

    /**
     * Set in LocalStackTestConfig
     */
    @Value("${test.aws.dynamodb.endpoint:#{null}}")
    String dynamoDbLocalStackEndpoint;

    /**
     * Set in LocalStackTestConfig
     */
    @Value("${test.aws.sns.endpoint:#{null}}")
    String snsLocalStackEndpoint;

    private static final DefaultAwsRegionProviderChain DEFAULT_AWS_REGION_PROVIDER_CHAIN = new DefaultAwsRegionProviderChain();
    private static final DefaultCredentialsProvider DEFAULT_CREDENTIALS_PROVIDER = DefaultCredentialsProvider.create();

//  <-- spring-cloud-starter-aws-messaging -->

    @Bean
    public QueueMessagingTemplate queueMessagingTemplate(final AmazonSQSAsync amazonSQSAsync) {
        return new QueueMessagingTemplate(amazonSQSAsync);
    }

    @Bean
    public QueueMessageHandlerFactory queueMessageHandlerFactory(final ObjectMapper objectMapper, final AmazonSQSAsync amazonSQSAsync) {

        final var queueHandlerFactory = new QueueMessageHandlerFactory();
        final var converter = new MappingJackson2MessageConverter();

        queueHandlerFactory.setAmazonSqs(amazonSQSAsync);
        converter.setObjectMapper(objectMapper);
        queueHandlerFactory.setArgumentResolvers(Collections.singletonList(new PayloadMethodArgumentResolver(converter)));

        return queueHandlerFactory;
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
    public DynamoDbAsyncClient dynamoDbAsyncClient() {
        DynamoDbAsyncClientBuilder dynamoDbAsyncClientBuilder = DynamoDbAsyncClient.builder()
                .credentialsProvider(DEFAULT_CREDENTIALS_PROVIDER);

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

    // TODO: In the future, delete these synchronous dynamo clients. Only asynchronous Dynamo clients will be used
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
    public SnsAsyncClient snsClient() {
        SnsAsyncClientBuilder snsAsyncClientBuilder = SnsAsyncClient.builder().credentialsProvider(DEFAULT_CREDENTIALS_PROVIDER);

        if (snsLocalStackEndpoint != null) {
            snsAsyncClientBuilder.region(Region.of(localStackRegion)).endpointOverride(URI.create(snsLocalStackEndpoint));
        } else {
            snsAsyncClientBuilder.region(DEFAULT_AWS_REGION_PROVIDER_CHAIN.getRegion());
        }

        return snsAsyncClientBuilder.build();
    }
}
