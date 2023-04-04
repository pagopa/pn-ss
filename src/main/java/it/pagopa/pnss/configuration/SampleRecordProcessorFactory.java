package it.pagopa.pnss.configuration;

import software.amazon.kinesis.processor.ShardRecordProcessor;
import software.amazon.kinesis.processor.ShardRecordProcessorFactory;

public class SampleRecordProcessorFactory implements ShardRecordProcessorFactory {
    private final String eventBridgeName;

    public SampleRecordProcessorFactory(String eventBridgeName) {
        this.eventBridgeName = eventBridgeName;
    }

    @Override
    public ShardRecordProcessor shardRecordProcessor() {
        return new SampleRecordProcessor(eventBridgeName);
    }
}
