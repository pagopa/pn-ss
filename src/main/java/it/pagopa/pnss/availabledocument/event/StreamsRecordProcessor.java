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
import it.pagopa.pnss.common.utils.EventBridgeUtil;
import it.pagopa.pnss.common.utils.LogUtils;
import it.pagopa.pnss.common.utils.SpringContext;
import it.pagopa.pnss.repositorymanager.entity.DocumentEntity;
import lombok.CustomLog;
import org.jetbrains.annotations.NotNull;
import org.slf4j.MDC;
import org.springframework.core.env.Environment;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import software.amazon.awssdk.services.eventbridge.EventBridgeClient;
import software.amazon.awssdk.services.eventbridge.model.EventBridgeException;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequest;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequestEntry;
import com.amazonaws.services.dynamodbv2.model.Record;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import static it.pagopa.pnss.common.constant.Constant.AVAILABLE;
import static it.pagopa.pnss.common.utils.EventBridgeUtil.*;
import static it.pagopa.pnss.common.utils.LogUtils.*;

@CustomLog
public class StreamsRecordProcessor implements IRecordProcessor {


    public static final String INSERT_EVENT = "INSERT";
    public static final String MODIFY_EVENT = "MODIFY";
    public static final String REMOVE_EVENT = "REMOVE";
    private static final String CAN_READ_TAGS = "canReadTags";
    private Integer checkpointCounter;
    private final EventBridgeClient eventBridgeClient = EventBridgeClient.create();
    private boolean test = false;
    private final String disponibilitaDocumentiEventBridge;
    DynamoDbAsyncClient dynamoDbClient;
    Environment environment = SpringContext.getBean(Environment.class);




    public StreamsRecordProcessor( String disponibilitaDocumentiEventBridge, DynamoDbAsyncClient dynamoDbClient) {
        this.disponibilitaDocumentiEventBridge = disponibilitaDocumentiEventBridge;
        this.dynamoDbClient = dynamoDbClient;
    }
    public StreamsRecordProcessor( String disponibilitaDocumentiEventBridge,DynamoDbAsyncClient dynamoDbClient, boolean test) {
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
        findEventSendToBridge(processRecordsInput)
                                .buffer(10)
                                .map(putEventsRequestEntries -> {

                                    PutEventsRequest eventsRequest = PutEventsRequest.builder()
                                            .entries(putEventsRequestEntries)
                                            .build();

                                    log.debug(CLIENT_METHOD_INVOCATION, "eventBridgeClient.putEvents()", eventsRequest);
                                    return eventBridgeClient.putEvents(eventsRequest);
                                })
                                .retryWhen(indefiniteRetry())
                                .then()
                                .doOnError(e -> log.fatal("DBStream: Errore generico ", e))
                                .doOnSuccess(unused -> {
                                    log.logEndingProcess(PROCESS_RECORDS);
                                    setCheckpoint(processRecordsInput);
                                })
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
                .filter(streamRecord -> {
                    String oldDocumentState = streamRecord.getDynamodb().getOldImage().get(DOCUMENTSTATE_KEY).getS();
                    String newDocumentState = streamRecord.getDynamodb().getNewImage().get(DOCUMENTSTATE_KEY).getS();
                    return !oldDocumentState.equalsIgnoreCase(newDocumentState) && newDocumentState.equalsIgnoreCase(AVAILABLE);
                })
                .flatMap(streamRecord -> {
                    var docEntity = streamRecord.getDynamodb().getNewImage();
                    MDC.put(MDC_CORR_ID_KEY, docEntity.get(DOCUMENTKEY_KEY).getS());
                    return MDCUtils.addMDCToContextAndExecute(getCanReadTags(streamRecord)
                            .mapNotNull(canReadTags -> {
                                PutEventsRequestEntry putEventsRequestEntry = EventBridgeUtil.createMessage((DocumentEntity) docEntity,
                                        disponibilitaDocumentiEventBridge,
                                        streamRecord.getDynamodb().getOldImage().get(DOCUMENTSTATE_KEY).getS(),
                                        canReadTags);
                                if (putEventsRequestEntry != null) {
                                    log.info("Event send to bridge {}", putEventsRequestEntry);
                                }
                                return putEventsRequestEntry;
                            }));
                })
                .doOnError(e -> log.fatal("DBStream: Errore generico nella gestione dell'evento - {}", e.getMessage(), e))
                .doOnComplete(() -> log.info("DBStream: Nessun evento da inviare a bridge"));
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
                log.info("shutdown - Setting checkpoint on shutdown");
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

    public Mono<Boolean> getCanReadTags(Record streamRecord) {
        final String METHOD_NAME = "getCanReadTags()";
        log.debug(LogUtils.INVOKING_METHOD, METHOD_NAME, Stream.of(streamRecord).toList());
        String cxId = streamRecord.getDynamodb().getNewImage().get("clientShortCode").getS();
        return Mono.fromSupplier(() -> {
                    boolean hasTags = hasTags(streamRecord);
                    boolean isClientInList = isClientInList(cxId);
                    boolean isCheckDisabled = isCheckDisabled();
                    if (!hasTags) {
                        log.debug("DBStream: Nessun tag presente nel record");
                    }
                    return ((isClientInList || isCheckDisabled) && hasTags) || (!isClientInList && !isCheckDisabled);
                })
                .filter(Boolean::booleanValue)
                .flatMap(unused -> Mono.fromCompletionStage(getFromDynamo(cxId)))
                .retryWhen(indefiniteRetry())
                .filter(getItemResponse -> getItemResponse.hasItem() && getItemResponse.item().containsKey(CAN_READ_TAGS))
                .map(getItemResponse -> getItemResponse.item().get(CAN_READ_TAGS).bool())
                .defaultIfEmpty(false)
                .doOnError(e -> log.debug(EXCEPTION_IN_PROCESS, METHOD_NAME, e))
                .doOnSuccess(result -> log.debug(SUCCESSFUL_OPERATION_LABEL, METHOD_NAME, result));
    }

    public CompletableFuture<GetItemResponse> getFromDynamo(String cxId){
        final String METHOD_NAME = "getFromDynamo()";
        log.debug(LogUtils.INVOKING_METHOD, METHOD_NAME, cxId);
        return dynamoDbClient.getItem(builder -> builder.tableName("pn-SsAnagraficaClient")
                .key(Map.of("name", AttributeValue.builder().s(cxId).build()))
                .projectionExpression(CAN_READ_TAGS));
    }

    private boolean isClientInList(String cxId) {
        List<String> clientList = getClientList();
        return clientList != null && clientList.contains(cxId);
    }

    private List<String> getClientList() {
        String clients = environment.getProperty("pn.ss.safe-clients");
        if (clients == null || clients.isEmpty()) {
            clients="";
        }
        return List.of(clients.strip().split(";"));
    }

    private boolean isCheckDisabled() {
        List<String> clientList = getClientList();
        return clientList != null && clientList.size() == 1 && "DISABLED".equals(clientList.get(0));
    }


    private boolean hasTags(Record inputRecord) {
        return inputRecord.getDynamodb().getNewImage().get("tags") != null && !inputRecord.getDynamodb().getNewImage().get("tags").getM().isEmpty();
    }


    private Retry indefiniteRetry() {
        return Retry.backoff(Long.MAX_VALUE, Duration.ofMillis(1000))
                .filter(throwable -> throwable instanceof DynamoDbException || throwable instanceof SdkClientException || throwable instanceof EventBridgeException)
                .doBeforeRetry(retrySignal -> log.warn(RETRY_ATTEMPT, retrySignal.totalRetries(), retrySignal.failure()+","+retrySignal.failure().getMessage()));
    }
}