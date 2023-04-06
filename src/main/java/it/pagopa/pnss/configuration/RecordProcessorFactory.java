package it.pagopa.pnss.configuration;

import software.amazon.kinesis.processor.ShardRecordProcessor;
import software.amazon.kinesis.processor.ShardRecordProcessorFactory;

public class RecordProcessorFactory implements ShardRecordProcessorFactory {
    private final String eventBridgeName;

    public RecordProcessorFactory(String eventBridgeName) {
        this.eventBridgeName = eventBridgeName;
    }


    @Override
    public ShardRecordProcessor shardRecordProcessor() {
        return new RecordProcessor();
    }
}
