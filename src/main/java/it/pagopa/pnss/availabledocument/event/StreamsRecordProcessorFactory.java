package it.pagopa.pnss.availabledocument.event;

import com.amazonaws.services.kinesis.clientlibrary.interfaces.v2.IRecordProcessor;
import com.amazonaws.services.kinesis.clientlibrary.interfaces.v2.IRecordProcessorFactory;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;


public class StreamsRecordProcessorFactory implements IRecordProcessorFactory {
    private final String disponibilitaDOcumentiEventBridge;
    private final DynamoDbAsyncClient dynamoDbClient;


    public StreamsRecordProcessorFactory(String disponibilitaDOcumentiEventBridge, DynamoDbAsyncClient dynamoDbClient) {
        this.dynamoDbClient = dynamoDbClient;
        this.disponibilitaDOcumentiEventBridge = disponibilitaDOcumentiEventBridge;
    }

    @Override
    public IRecordProcessor createProcessor() {
        return new StreamsRecordProcessor( disponibilitaDOcumentiEventBridge, dynamoDbClient);
    }
}
