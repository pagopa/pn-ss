package it.pagopa.pnss.availableDocument.event;

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
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.InitialPositionInStream;
import it.pagopa.pn.commons.configs.aws.AwsConfigs;
import it.pagopa.pnss.configurationproperties.AvailabelDocumentEventBridgeName;
import it.pagopa.pnss.configurationproperties.DynamoEventStreamName;
import it.pagopa.pnss.configurationproperties.RepositoryManagerDynamoTableName;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.KinesisClientLibConfiguration;
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.Worker;
import javax.annotation.PostConstruct;
import com.amazonaws.services.kinesis.clientlibrary.interfaces.v2.IRecordProcessorFactory;

@Component
@Slf4j
public class KinesisClientWorker  {
    private final AwsConfigs props;
    private static IRecordProcessorFactory recordProcessorFactory;
    private static String table;
    private static String documentName;

    private static String disponibilitaDocumentiEventBridge;
    DynamoEventStreamName dynamoEventStreamName;

    public KinesisClientWorker(AwsConfigs props,
                               RepositoryManagerDynamoTableName repositoryManagerDynamoTableName,
                               AvailabelDocumentEventBridgeName availabelDocumentEventBridgeName,
                               DynamoEventStreamName dynamoEventStreamName) {
        this.props = props;
        this.table = repositoryManagerDynamoTableName.documentiName();
        this.disponibilitaDocumentiEventBridge= availabelDocumentEventBridgeName.disponibilitaDocumentiName();
        this.documentName = dynamoEventStreamName.documentName();
    }


    @PostConstruct
    public  void run(){



        AWSCredentialsProvider awsCredentialsProvider = DefaultAWSCredentialsProviderChain.getInstance();
        AmazonDynamoDB amazonDynamoDB = AmazonDynamoDBClientBuilder.standard()
                .withRegion(props.getRegionCode())
                .build();
        AmazonCloudWatch cloudWatchClient = AmazonCloudWatchClientBuilder.standard()
                .withRegion(props.getRegionCode())
                .build();
        AmazonDynamoDBStreams dynamoDBStreamsClient = AmazonDynamoDBStreamsClientBuilder.standard()
                .withRegion(props.getRegionCode())
                .build();
        AmazonDynamoDBStreamsAdapterClient adapterClient = new AmazonDynamoDBStreamsAdapterClient(dynamoDBStreamsClient);
        KinesisClientLibConfiguration workerConfig = new KinesisClientLibConfiguration(
                "streams-adapter-demo",
                //"arn:aws:dynamodb:eu-central-1:713024823233:table/dgs-bing-ss-PnSsTableDocumenti-1TTOSMTT2OCLC/stream/2023-02-08T18:29:08.530",
                documentName,
                awsCredentialsProvider,
                "streams-demo-worker")
                .withMaxLeaseRenewalThreads(5000)
                .withMaxLeasesForWorker(5000)
                .withMaxRecords(1000)
                .withIdleTimeBetweenReadsInMillis(500)
                .withInitialPositionInStream(InitialPositionInStream.TRIM_HORIZON);

        recordProcessorFactory = new StreamsRecordProcessorFactory(amazonDynamoDB, table, disponibilitaDocumentiEventBridge);
        Worker worker  = StreamsWorkerFactory.createDynamoDbStreamsWorker(recordProcessorFactory, workerConfig, adapterClient, amazonDynamoDB, cloudWatchClient);
        Thread t = new Thread(worker);
        t.start();
    }
}
