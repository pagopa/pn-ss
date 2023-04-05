package it.pagopa.pnss.configuration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.kinesis.exceptions.InvalidStateException;
import software.amazon.kinesis.exceptions.ShutdownException;
import software.amazon.kinesis.lifecycle.events.*;
import software.amazon.kinesis.processor.ShardRecordProcessor;

import java.util.HashMap;


public class TestRecordProcessor implements ShardRecordProcessor {

    private static final String SHARD_KEY = "ShardId";
    private static final Logger log = LoggerFactory.getLogger(TestRecordProcessor.class);
    private HashMap<String, String> shardMap;
    private String shardId;

    public TestRecordProcessor() {
        this.shardMap = new HashMap();
    }

    public void initialize(InitializationInput initializationInput) {
        shardId = initializationInput.shardId();
        shardMap.put(SHARD_KEY, shardId);
        try {
            log.info("Initializing @ Sequence: {}", initializationInput.extendedSequenceNumber());
        } finally {
            shardMap.remove(SHARD_KEY);
        }
    }

    public void processRecords(ProcessRecordsInput processRecordsInput) {
        shardMap.put(SHARD_KEY, shardId);
        try {
            log.info("Thread Name:" + Thread.currentThread().getName() + " Processing {} record(s)", processRecordsInput.records().size());
            processRecordsInput.records().forEach(r -> log.info("Processing record pk: {} -- Seq: {} -- Data: {}", r.partitionKey(), r.sequenceNumber(), r.data()));
        } catch (Throwable t) {
            log.error("Caught throwable while processing records.  Aborting");
            Runtime.getRuntime().halt(1);
        } finally {
            shardMap.remove(SHARD_KEY);
        }
    }

    public void leaseLost(LeaseLostInput leaseLostInput) {
        log.error("leaseLostInput ", leaseLostInput.toString());
    }

    public void shardEnded(ShardEndedInput shardEndedInput) {
        shardMap.put(SHARD_KEY, shardId);
        try {
            log.info("Reached shard end checkpointing.");
            shardEndedInput.checkpointer().checkpoint();
        } catch (ShutdownException | InvalidStateException e) {
            log.error("Exception while checkpointing at shard end.  Giving up", e);
        } finally {
            shardMap.remove(SHARD_KEY);
        }

    }

    public void shutdownRequested(ShutdownRequestedInput shutdownRequestedInput) {
        shardMap.put(SHARD_KEY, shardId);
        try {
            log.info("Scheduler is shutting down, checkpointing.");
            shutdownRequestedInput.checkpointer().checkpoint();
        } catch (ShutdownException | InvalidStateException e) {
            log.error("Exception while checkpointing at requested shutdown.  Giving up", e);
        } finally {
            shardMap.remove(SHARD_KEY);
        }
    }
}
