package it.pagopa.pnss.availabledocument.event;

import com.amazonaws.services.kinesis.clientlibrary.interfaces.v2.IRecordProcessor;
import com.amazonaws.services.kinesis.clientlibrary.interfaces.v2.IRecordProcessorFactory;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;


public class StreamsRecordProcessorFactory implements IRecordProcessorFactory {
    private final String disponibilitaDOcumentiEventBridge;
    private final DynamoDbClient dynamoDbClient;


    public StreamsRecordProcessorFactory(String disponibilitaDOcumentiEventBridge, DynamoDbClient dynamoDbClient) {
        this.dynamoDbClient = dynamoDbClient;
        this.disponibilitaDOcumentiEventBridge = disponibilitaDOcumentiEventBridge;
    }

    @Override
    public IRecordProcessor createProcessor() {
        return new StreamsRecordProcessor( disponibilitaDOcumentiEventBridge, dynamoDbClient);
    }
}
