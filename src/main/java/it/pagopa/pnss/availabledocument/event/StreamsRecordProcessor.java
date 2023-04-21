package it.pagopa.pnss.availabledocument.event;

import com.amazonaws.services.dynamodbv2.streamsadapter.model.RecordAdapter;
import com.amazonaws.services.kinesis.clientlibrary.interfaces.v2.IRecordProcessor;
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.ShutdownReason;
import com.amazonaws.services.kinesis.clientlibrary.types.InitializationInput;
import com.amazonaws.services.kinesis.clientlibrary.types.ProcessRecordsInput;
import com.amazonaws.services.kinesis.clientlibrary.types.ShutdownInput;
import com.amazonaws.services.kinesis.model.Record;
import it.pagopa.pnss.common.exception.PutEventsRequestEntryException;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import software.amazon.awssdk.services.eventbridge.EventBridgeClient;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequest;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequestEntry;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class StreamsRecordProcessor implements IRecordProcessor {
    public static final String INSERT_EVENT = "INSERT";
    public static final String MODIFY_EVENT = "MODIFY";
    public static final String REMOVE_EVENT = "REMOVE";
    private Integer checkpointCounter;

    private boolean test = false;
    private final String disponibilitaDocumentiEventBridge;

    public StreamsRecordProcessor( String disponibilitaDocumentiEventBridge) {
        this.disponibilitaDocumentiEventBridge = disponibilitaDocumentiEventBridge;

    }
    public StreamsRecordProcessor( String disponibilitaDocumentiEventBridge, boolean test) {
        this.disponibilitaDocumentiEventBridge = disponibilitaDocumentiEventBridge;
        this.test = test;
    }
    @Override
    public void initialize(InitializationInput initializationInput) {
        checkpointCounter = 0;
    }

    @Override
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
            log.error("DBStream: Errore generico ",e);
        }

    }

    @NotNull
    public List<PutEventsRequestEntry> findEventSendToBridge (ProcessRecordsInput processRecordsInput) {
        List<PutEventsRequestEntry> requestEntries = new ArrayList<>();
        for (Record recordEvent : processRecordsInput.getRecords()) {
            String data = new String(recordEvent.getData().array(), Charset.forName("UTF-8"));
            log.debug("DBStream: {}", data);
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

                    log.debug("DBStream Ok: {}", recordEvent.getPartitionKey());

                }catch (Exception ex){
                    log.error("DBStream: Errore generico nella gestione dell'evento su {} - {}",recordEvent.getPartitionKey(), ex );
                }
            }

        }
        try {
            if (!test){
                processRecordsInput.getCheckpointer().checkpoint();
            }
        }
        catch (Exception e) {
            log.error("DBStream: Errore nel settaggio del checkpoint",e );
            throw new PutEventsRequestEntryException(PutEventsRequestEntry.class);
        }
        return requestEntries;
    }

    @Override
    public void shutdown(ShutdownInput shutdownInput) {
        if (shutdownInput.getShutdownReason() == ShutdownReason.TERMINATE) {
            try {
                shutdownInput.getCheckpointer().checkpoint();
            }
            catch (Exception e) {
                log.error("DBStream: Errore durante il processo di shutDown", e);
            }
        }

    }
}