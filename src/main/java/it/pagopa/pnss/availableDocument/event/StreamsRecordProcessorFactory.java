package it.pagopa.pnss.availableDocument.event;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.kinesis.clientlibrary.interfaces.v2.IRecordProcessor;
import com.amazonaws.services.kinesis.clientlibrary.interfaces.v2.IRecordProcessorFactory;
import it.pagopa.pnss.configurationproperties.RepositoryManagerDynamoTableName;
import org.springframework.stereotype.Service;

public class StreamsRecordProcessorFactory implements IRecordProcessorFactory {
    private final String tableName;
    private String disponibilitaDOcumentiEventBridge;
    private final AmazonDynamoDB dynamoDBClient;

    public StreamsRecordProcessorFactory(AmazonDynamoDB dynamoDBClient, String tableName, String disponibilitaDOcumentiEventBridge) {
        this.tableName = tableName;
        this.dynamoDBClient = dynamoDBClient;
        this.disponibilitaDOcumentiEventBridge = disponibilitaDOcumentiEventBridge;
    }

    @Override
    public IRecordProcessor createProcessor() {
        return new StreamsRecordProcessor(dynamoDBClient, tableName, disponibilitaDOcumentiEventBridge);
    }
}
