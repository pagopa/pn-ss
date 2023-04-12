package it.pagopa;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.PropertySource;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.dynamodb.model.ShardIteratorType;
import software.amazon.awssdk.services.dynamodb.streams.DynamoDbStreamsAsyncClient;

@SpringBootApplication
@ConfigurationPropertiesScan

//<-- COMMONS -->
//AWS CONFIGURATION
@PropertySource("classpath:commons/aws-configuration.properties")

//<-- REPOSITORY MANAGER -->
//DYNAMO TABLES
@PropertySource("classpath:repositorymanager/repository-manager-dynamo-table.properties")

// BUCKET
@PropertySource("classpath:bucket/bucket.properties")

// QUEUE
@PropertySource("classpath:queue/queue.properties")

// EVENT STREAM
@PropertySource("classpath:eventStream/dynamo-event-stream.properties")

// EVENT BRIDGE
@PropertySource("classpath:eventBridge/event-bridge-disponibilita-documenti.properties")

// INTERNAL ENDPOINTS
@PropertySource("classpath:commons/internal-endpoint.properties")
@Slf4j
public class SsApplication {

    public static void main(String[] args) {
        SpringApplication.run(SsApplication.class, args);
    }

    private static final String STREAM_ARN =
            "arn:aws:dynamodb:eu-central-1:713024823233:table/dgs-bing-ss-PnSsTableDocumenti-1TTOSMTT2OCLC/stream/2023-02-08T18:29:08.530";

    @Bean
    CommandLineRunner commandLineRunner(DynamoDbStreamsAsyncClient dynamoDbStreamsAsyncClient) {
        return args -> Mono.fromCompletionStage(dynamoDbStreamsAsyncClient.describeStream(builder -> builder.streamArn(STREAM_ARN)))
                           .flatMapIterable(describeStreamResponse -> describeStreamResponse.streamDescription().shards())
                           .map(shard -> shard)
                           .flatMap(shard -> Mono.fromCompletionStage(dynamoDbStreamsAsyncClient.getShardIterator(builder -> builder.streamArn(
                                   STREAM_ARN).shardIteratorType(ShardIteratorType.LATEST).shardId(shard.shardId()))))
                           .flatMap(getShardIteratorResponse -> Mono.fromCompletionStage(dynamoDbStreamsAsyncClient.getRecords(builder -> builder.shardIterator(
                                   getShardIteratorResponse.shardIterator()))))
                           .map(getRecordsResponse -> getRecordsResponse)
                           .subscribe();
    }
}
