package it.pagopa.pnss.availabledocument.configuration;

import it.pagopa.pnss.common.configurationproperties.AvailabelDocumentEventBridgeName;
import it.pagopa.pnss.common.configurationproperties.DynamoEventStreamName;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.kinesis.KinesisAsyncClient;
import software.amazon.kinesis.common.ConfigsBuilder;
import software.amazon.kinesis.common.StreamIdentifier;
import software.amazon.kinesis.coordinator.Scheduler;
import software.amazon.kinesis.processor.ShardRecordProcessor;
import software.amazon.kinesis.processor.ShardRecordProcessorFactory;

import java.util.UUID;

@Configuration
public class AvailableDocumentService {

    private final DynamoEventStreamName dynamoEventStreamName;
    private final AvailabelDocumentEventBridgeName availabelDocumentEventBridgeName;

    @Value("${enable.availabledocument:true}")
    private String enableAvailableDocument;

    public AvailableDocumentService(DynamoEventStreamName dynamoEventStreamName,
                                    AvailabelDocumentEventBridgeName availabelDocumentEventBridgeName) {
        this.dynamoEventStreamName = dynamoEventStreamName;
        this.availabelDocumentEventBridgeName = availabelDocumentEventBridgeName;
    }

//    private static CompletableFuture<Void> responseHandlerBuilder(KinesisAsyncClient client) {
//        SubscribeToShardRequest request = SubscribeToShardRequest.builder()
//                                                                 .consumerARN(
//                                                                         "arn:aws:dynamodb:eu-central-1:713024823233:table/dgs-bing-ss" +
//                                                                         "-PnSsTableDocumenti-1TTOSMTT2OCLC/stream/2023-02-08T18:29:08
//                                                                         .530")
//                                                                 .shardId
//                                                                 ("arn:aws:kinesis:us-east-1:111122223333:stream/StockTradeStream")
//                                                                 .startingPosition(s -> s.type(ShardIteratorType.LATEST))
//                                                                 .build();
//
//        SubscribeToShardResponseHandler responseHandler = SubscribeToShardResponseHandler.builder()
//                                                                                         .onError(t -> System.err.println(
//                                                                                                 "Error during stream - " + t
//                                                                                                 .getMessage()))
//                                                                                         .onEventStream(p -> Flux.from(p)
//                                                                                                                 .ofType
//                                                                                                                 (SubscribeToShardEvent
//                                                                                                                 .class)
//                                                                                                                 .flatMapIterable(
//                                                                                                                         SubscribeToShardEvent::records)
//                                                                                                                 .limit(1000)
//                                                                                                                 .buffer(25)
//                                                                                                                 .subscribe(e -> System
//                                                                                                                 .out.println(
//                                                                                                                         "Record batch
//                                                                                                                         = " +
//                                                                                                                         e)))
//                                                                                         .build();
//
//        return client.subscribeToShard(request, responseHandler);
//    }

    @Bean
    public CommandLineRunner schedulingRunner(@Qualifier("taskExecutor") TaskExecutor executor, KinesisAsyncClient kinesisAsyncClient,
                                              DynamoDbAsyncClient dynamoDbAsyncClient, CloudWatchAsyncClient cloudWatchAsyncClient) {
        return args -> {

            ConfigsBuilder configsBuilder = new ConfigsBuilder(dynamoEventStreamName.documentName(),
                                                               dynamoEventStreamName.tableMetadata(),
                                                               kinesisAsyncClient,
                                                               dynamoDbAsyncClient,
                                                               cloudWatchAsyncClient,
                                                               UUID.randomUUID().toString(),
                                                               new ShardRecordProcessorFactory() {

                                                                   @Override
                                                                   public ShardRecordProcessor shardRecordProcessor() {
                                                                       return null;
                                                                   }



                                                                   @Override
                                                                   public ShardRecordProcessor shardRecordProcessor(StreamIdentifier streamIdentifier) {
                                                                       return ShardRecordProcessorFactory.super.shardRecordProcessor(
                                                                               streamIdentifier);
                                                                   }
                                                               });

            Scheduler scheduler = new Scheduler(configsBuilder.checkpointConfig(),
                                                configsBuilder.coordinatorConfig(),
                                                configsBuilder.leaseManagementConfig(),
                                                configsBuilder.lifecycleConfig(),
                                                configsBuilder.metricsConfig(),
                                                configsBuilder.processorConfig(),
                                                configsBuilder.retrievalConfig());

            if (enableAvailableDocument == null) {
                executor.execute(scheduler);
            }
        };
    }
}
