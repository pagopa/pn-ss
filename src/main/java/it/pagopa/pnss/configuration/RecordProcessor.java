package it.pagopa.pnss.configuration;

import com.amazonaws.services.dynamodbv2.streamsadapter.model.RecordAdapter;
import it.pagopa.pnss.availabledocument.event.ManageDynamoEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.eventbridge.EventBridgeClient;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequest;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequestEntry;
import software.amazon.kinesis.exceptions.InvalidStateException;
import software.amazon.kinesis.exceptions.ShutdownException;
import software.amazon.kinesis.lifecycle.events.*;
import software.amazon.kinesis.processor.ShardRecordProcessor;

import javax.validation.constraints.NotNull;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


public class RecordProcessor implements ShardRecordProcessor {

    public static final String INSERT_EVENT = "INSERT";
    public static final String MODIFY_EVENT = "MODIFY";
    public static final String REMOVE_EVENT = "REMOVE";
    private static final String SHARD_KEY = "ShardId";
    private static final Logger log = LoggerFactory.getLogger(RecordProcessor.class);
    private HashMap<String, String> shardMap;
    private String shardId;

    public RecordProcessor() {
        this.shardMap = new HashMap();
    }

    public void initialize(InitializationInput initializationInput) {
        shardId = initializationInput.shardId();
        shardMap.put(SHARD_KEY, shardId);
        try {
            log.debug("Initializing @ Sequence: {}", initializationInput.extendedSequenceNumber());
        } finally {
            shardMap.remove(SHARD_KEY);
        }
    }

    public void processRecords(ProcessRecordsInput processRecordsInput) {
        try {
            List<PutEventsRequestEntry> requestEntries = findEventSendToBridge(processRecordsInput);
            if (!requestEntries.isEmpty()) {
                EventBridgeClient eventBridgeClient =
                        EventBridgeClient.builder()
                                //.credentialsProvider()
                                //.defaultsMode()
                                //.dualstackEnabled()
                                //.endpointOverride()
                                //.httpClient()
                                //.httpClientBuilder()
                                .build();
                PutEventsRequest eventsRequest = PutEventsRequest.builder()
                        .entries(requestEntries)
                        .build();
                eventBridgeClient.putEvents(eventsRequest);

            }
        }catch (Exception e ){
            log.error("Errore generico ",e);
        }

    }

    @NotNull
    public List<PutEventsRequestEntry> findEventSendToBridge (ProcessRecordsInput processRecordsInput) {
        List<PutEventsRequestEntry> requestEntries = new ArrayList<>();
        for (Record recordEvent : processRecordsInput.getRecords()) {
            String data = new String(recordEvent.getData().array(), Charset.forName("UTF-8"));
            log.info(data);
            log.info("--- START ---");
            if (recordEvent instanceof RecordAdapter) {
                com.amazonaws.services.dynamodbv2.model.Record streamRecord = ((RecordAdapter) recordEvent)
                        .getInternalObject();
                PutEventsRequestEntry putEventsRequestEntry = null;
                try{
                    switch (streamRecord.getEventName()) {
                        case INSERT_EVENT:
                            break;
                        case MODIFY_EVENT:
                            ManageDynamoEvent mde = new ManageDynamoEvent();
                            putEventsRequestEntry = mde.manageItem(disponibilitaDocumentiEventBridge,
                                    streamRecord.getDynamodb().getNewImage(), streamRecord.getDynamodb().getOldImage());
                            if (putEventsRequestEntry!=null ){
                                requestEntries.add(putEventsRequestEntry);
                            }
                            break;
                        case REMOVE_EVENT:
                            break;
                    }

                    log.info("--- COMPLETATO CON SUCCESSO  ---");

                }catch (Exception ex){
                    log.error("Errore generico nella gestione dell'evento ",ex );
                    log.info("--- COMPLETATO CON ERRORE  ---");
                }
            }

        }
        try {
            if (!test){
                processRecordsInput.getCheckpointer().checkpoint();
            }
        }
        catch (Exception e) {
            log.error("Errore nella commit dell'evento non mando nessun evento nuovo",e );
            throw new PutEventsRequestEntryException(PutEventsRequestEntry.class);
        }
        return requestEntries;
    }

    public void leaseLost(LeaseLostInput leaseLostInput) {
        log.error("leaseLostInput ", leaseLostInput.toString());
    }

    public void shardEnded(ShardEndedInput shardEndedInput) {
        shardMap.put(SHARD_KEY, shardId);
        try {
            log.debug("Reached shard end checkpointing.");
            shardEndedInput.checkpointer().checkpoint();
        } catch (ShutdownException | InvalidStateException e) {
            log.debug("Exception while checkpointing at shard end.  Giving up", e);
        } finally {
            shardMap.remove(SHARD_KEY);
        }

    }

    public void shutdownRequested(ShutdownRequestedInput shutdownRequestedInput) {
        shardMap.put(SHARD_KEY, shardId);
        try {
            log.debug("Scheduler is shutting down, checkpointing.");
            shutdownRequestedInput.checkpointer().checkpoint();
        } catch (ShutdownException | InvalidStateException e) {
            log.debug("Exception while checkpointing at requested shutdown.  Giving up", e);
        } finally {
            shardMap.remove(SHARD_KEY);
        }
    }
}