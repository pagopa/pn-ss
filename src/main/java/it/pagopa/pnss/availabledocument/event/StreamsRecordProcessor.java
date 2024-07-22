package it.pagopa.pnss.availabledocument.event;

import com.amazonaws.services.dynamodbv2.streamsadapter.model.RecordAdapter;
import com.amazonaws.services.kinesis.clientlibrary.exceptions.ShutdownException;
import com.amazonaws.services.kinesis.clientlibrary.exceptions.ThrottlingException;
import com.amazonaws.services.kinesis.clientlibrary.interfaces.v2.IRecordProcessor;
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.ShutdownReason;
import com.amazonaws.services.kinesis.clientlibrary.types.InitializationInput;
import com.amazonaws.services.kinesis.clientlibrary.types.ProcessRecordsInput;
import com.amazonaws.services.kinesis.clientlibrary.types.ShutdownInput;
import it.pagopa.pn.commons.utils.MDCUtils;
import it.pagopa.pnss.common.exception.PutEventsRequestEntryException;
import lombok.CustomLog;
import org.jetbrains.annotations.NotNull;
import org.slf4j.MDC;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import software.amazon.awssdk.services.eventbridge.EventBridgeClient;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequest;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequestEntry;

import java.util.Map;

import static it.pagopa.pnss.common.utils.LogUtils.*;

@CustomLog
public class StreamsRecordProcessor implements IRecordProcessor {


    public static final String INSERT_EVENT = "INSERT";
    public static final String MODIFY_EVENT = "MODIFY";
    public static final String REMOVE_EVENT = "REMOVE";
    private Integer checkpointCounter;
    private final EventBridgeClient eventBridgeClient = EventBridgeClient.create();
    private boolean test = false;
    private final String disponibilitaDocumentiEventBridge;
    DynamoDbClient dynamoDbClient;



    public StreamsRecordProcessor( String disponibilitaDocumentiEventBridge, DynamoDbClient dynamoDbClient) {
        this.disponibilitaDocumentiEventBridge = disponibilitaDocumentiEventBridge;
          this.dynamoDbClient = dynamoDbClient;
    }
    public StreamsRecordProcessor( String disponibilitaDocumentiEventBridge,DynamoDbClient dynamoDbClient, boolean test) {
        this.disponibilitaDocumentiEventBridge = disponibilitaDocumentiEventBridge;
        this.test = test;
        this.dynamoDbClient = dynamoDbClient;
    }
    @Override
    public void initialize(InitializationInput initializationInput) {
        checkpointCounter = 0;
    }

    @Override
    public void processRecords(ProcessRecordsInput processRecordsInput) {
        final String PROCESS_RECORDS = "processRecords()";
        MDC.clear();
        log.logStartingProcess(PROCESS_RECORDS);
        MDCUtils.addMDCToContextAndExecute(
                findEventSendToBridge(processRecordsInput)
                .buffer(10)
                .map(putEventsRequestEntries -> {

                    PutEventsRequest eventsRequest = PutEventsRequest.builder()
                            .entries(putEventsRequestEntries)
                            .build();

                    log.debug(CLIENT_METHOD_INVOCATION, "eventBridgeClient.putEvents()", eventsRequest);
                    return eventBridgeClient.putEvents(eventsRequest);
                })
                .then()
                .doOnError(e -> log.fatal("DBStream: Errore generico ", e))
                .doOnSuccess(unused -> {
                    log.logEndingProcess(PROCESS_RECORDS);
                    setCheckpoint(processRecordsInput);
                }))
                .subscribe();
    }

    @NotNull
    public Flux<PutEventsRequestEntry> findEventSendToBridge(ProcessRecordsInput processRecordsInput) {
        final String FIND_EVENT_SEND_TO_BRIDGE = "StreamRecordProcessor.findEventSendToBridge()";
        log.debug(INVOKING_METHOD, FIND_EVENT_SEND_TO_BRIDGE, processRecordsInput);
        return Flux.fromIterable(processRecordsInput.getRecords())
                .filter(RecordAdapter.class::isInstance)
                .map(recordEvent -> ((RecordAdapter) recordEvent).getInternalObject())
                .filter(streamRecord -> streamRecord.getEventName().equals(MODIFY_EVENT))
                .flatMap(streamRecord -> {String cxId = streamRecord.getDynamodb().getNewImage().get("clientShortCode").getS();
                    Boolean canReadTags = getCanReadTags(cxId);
                    ManageDynamoEvent mde = new ManageDynamoEvent();
                    PutEventsRequestEntry putEventsRequestEntry = mde.manageItem(disponibilitaDocumentiEventBridge,
                            streamRecord.getDynamodb().getNewImage(), streamRecord.getDynamodb().getOldImage(), canReadTags);
                    if (putEventsRequestEntry != null) {
                        log.info("Event send to bridge {}", putEventsRequestEntry);

                    }
                    return Mono.justOrEmpty(putEventsRequestEntry);
                })
                .doOnError(e -> log.fatal("DBStream: Errore generico nella gestione dell'evento - {}", e.getMessage(), e))
                .doOnComplete(() -> {
                    log.info(SUCCESSFUL_OPERATION_LABEL, FIND_EVENT_SEND_TO_BRIDGE, processRecordsInput);
                });
    }


    private void setCheckpoint(ProcessRecordsInput processRecordsInput) {
        try {
            if (!test) {

                    log.info("Setting checkpoint on id {}", processRecordsInput.getRecords().get(processRecordsInput.getRecords().size() - 1));

                    processRecordsInput.getCheckpointer().checkpoint();

            }
        } catch (ShutdownException se) {
            log.info("processRecords - Encountered shutdown exception, skipping checkpoint: {} {}", se, se.getMessage());
        } catch (ThrottlingException te) {
            log.info("processRecords - Encountered throttling exception, skipping checkpoint: {} {}", te, te.getMessage());
        } catch (Exception e) {
            log.fatal("Error while tring to set checkpoint: {} {} {}", e, processRecordsInput, e.getMessage());
            throw new PutEventsRequestEntryException(PutEventsRequestEntry.class);
        }
    }

    @Override
    public void shutdown(ShutdownInput shutdownInput) {
        if (shutdownInput.getShutdownReason() == ShutdownReason.TERMINATE) {
            try {
                shutdownInput.getCheckpointer().checkpoint();
            } catch (ShutdownException se) {
                log.info("shutdown - Encountered shutdown exception, skipping checkpoint: {} {}", se, se.getMessage());
            } catch (ThrottlingException te) {
                log.info("shutdown - Encountered throttling exception, skipping checkpoint: {} {}", te, te.getMessage());
            } catch (Exception e) {
                log.fatal("DBStream: Error while trying to shutdown checkpoint: {} {} {}",  e , shutdownInput.getShutdownReason(), e.getMessage());
            }
        }
    }

    private Boolean getCanReadTags(String cxId) {

        GetItemResponse getItemResponse = dynamoDbClient.getItem(builder -> builder.tableName("pn-SsAnagraficaClient")
                .key(Map.of("name", AttributeValue.builder().s(cxId).build()))
                .projectionExpression("canReadTags"));

        return getItemResponse.item().get("canReadTags").bool();
    }



}