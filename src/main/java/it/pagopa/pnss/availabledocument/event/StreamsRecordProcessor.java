package it.pagopa.pnss.availabledocument.event;

import com.amazonaws.services.dynamodbv2.streamsadapter.model.RecordAdapter;
import com.amazonaws.services.kinesis.clientlibrary.exceptions.ShutdownException;
import com.amazonaws.services.kinesis.clientlibrary.interfaces.v2.IRecordProcessor;
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.ShutdownReason;
import com.amazonaws.services.kinesis.clientlibrary.types.InitializationInput;
import com.amazonaws.services.kinesis.clientlibrary.types.ProcessRecordsInput;
import com.amazonaws.services.kinesis.clientlibrary.types.ShutdownInput;
import com.amazonaws.services.kinesis.model.Record;
import it.pagopa.pnss.common.constant.Constant;
import it.pagopa.pnss.common.exception.PutEventsRequestEntryException;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.eventbridge.EventBridgeClient;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequest;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequestEntry;
import java.nio.charset.StandardCharsets;

import static it.pagopa.pnss.common.constant.Constant.CLIENT_METHOD_INVOCATION;

@Slf4j
public class StreamsRecordProcessor implements IRecordProcessor {
    public static final String INSERT_EVENT = "INSERT";
    public static final String MODIFY_EVENT = "MODIFY";
    public static final String REMOVE_EVENT = "REMOVE";
    private Integer checkpointCounter;
    private final EventBridgeClient eventBridgeClient = EventBridgeClient.create();
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
        final String PROCESS_RECORDS = "processRecords";
        log.debug(Constant.INVOKING_METHOD + Constant.ARG, PROCESS_RECORDS, processRecordsInput, processRecordsInput.getMillisBehindLatest());
        findEventSendToBridge(processRecordsInput)
                .buffer(10)
                .map(putEventsRequestEntries -> {

                    PutEventsRequest eventsRequest = PutEventsRequest.builder()
                            .entries(putEventsRequestEntries)
                            .build();

                    log.info(CLIENT_METHOD_INVOCATION, "eventBridgeClient.putEvents()", eventsRequest);
                    return eventBridgeClient.putEvents(eventsRequest);
                })
                .then()
                .doOnError(e -> log.error("* FATAL * DBStream: Errore generico ", e))
                .doOnSuccess(unused -> log.info(Constant.SUCCESSFUL_OPERATION_LABEL, PROCESS_RECORDS, "StreamsRecordProcessor.processRecords()", processRecordsInput))
                .subscribe();
    }

    @NotNull
    public Flux<PutEventsRequestEntry> findEventSendToBridge(ProcessRecordsInput processRecordsInput) {
        final String FIND_EVENT_SEND_TO_BRIDGE = "findEventSendToBridge";
        log.debug(Constant.INVOKING_METHOD + Constant.ARG, FIND_EVENT_SEND_TO_BRIDGE, processRecordsInput, processRecordsInput.getRecords().size());
        return Flux.fromIterable(processRecordsInput.getRecords())
                .filter(RecordAdapter.class::isInstance)
                .map(recordEvent -> ((RecordAdapter) recordEvent).getInternalObject())
                .filter(streamRecord -> streamRecord.getEventName().equals(MODIFY_EVENT))
                .flatMap(streamRecord -> {
                    ManageDynamoEvent mde = new ManageDynamoEvent();
                    return Mono.justOrEmpty(mde.manageItem(disponibilitaDocumentiEventBridge,
                            streamRecord.getDynamodb().getNewImage(), streamRecord.getDynamodb().getOldImage()));
                })
                .doOnError(e -> log.error("* FATAL * DBStream: Errore generico nella gestione dell'evento - {}", e.getMessage(), e))
                .doOnComplete(() -> {
                    setCheckpoint(processRecordsInput);
                    log.info(Constant.SUCCESSFUL_OPERATION_LABEL, FIND_EVENT_SEND_TO_BRIDGE, "StreamsRecordProcessor.findEventSendToBridge()", processRecordsInput);
                });
    }

    private void setCheckpoint(ProcessRecordsInput processRecordsInput) {
        try {
            if (!test) {
                try {
                    processRecordsInput.getCheckpointer().checkpoint();
                } catch (ShutdownException e) {
                    log.info("processRecords - checkpointing: {} {}", e, e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("* FATAL * processRecords: {} {}", e, e.getMessage());
            throw new PutEventsRequestEntryException(PutEventsRequestEntry.class);
        }
    }

    @Override
    public void shutdown(ShutdownInput shutdownInput) {
        if (shutdownInput.getShutdownReason() == ShutdownReason.TERMINATE) {
            try {
                shutdownInput.getCheckpointer().checkpoint();
            }
            catch (Exception e) {
                log.error("* FATAL * DBStream: Errore durante il processo di shutDown", e);
            }
        }

    }
}