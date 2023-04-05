package it.pagopa.pnss.configuration;

import software.amazon.kinesis.common.StreamIdentifier;
import software.amazon.kinesis.lifecycle.events.*;
import software.amazon.kinesis.processor.ShardRecordProcessor;
import software.amazon.kinesis.processor.ShardRecordProcessorFactory;

public class SampleRecordProcessorFactory implements ShardRecordProcessorFactory {
    private final String eventBridgeName;

    public SampleRecordProcessorFactory(String eventBridgeName) {
        this.eventBridgeName = eventBridgeName;
    }


    @Override
    public ShardRecordProcessor shardRecordProcessor() {
        return new TestRecordProcessor();
    }
}
