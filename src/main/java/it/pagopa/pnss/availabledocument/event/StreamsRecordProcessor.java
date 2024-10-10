package it.pagopa.pnss.availabledocument.event;

import it.pagopa.pn.commons.utils.MDCUtils;
import it.pagopa.pnss.common.model.dto.DocumentStateDto;
import it.pagopa.pnss.common.model.pojo.SqsMessageWrapper;
import it.pagopa.pnss.common.service.SqsService;
import it.pagopa.pnss.common.utils.EventBridgeUtil;
import it.pagopa.pnss.common.utils.LogUtils;
import it.pagopa.pnss.configurationproperties.StreamRecordProcessorQueueName;
import it.pagopa.pnss.configurationproperties.retry.SqsEventHandlerRetryStrategyProperties;
import it.pagopa.pnss.repositorymanager.entity.DocumentEntity;
import lombok.CustomLog;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;
import reactor.util.retry.Retry;
import reactor.util.retry.RetryBackoffSpec;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import software.amazon.awssdk.services.eventbridge.EventBridgeClient;
import software.amazon.awssdk.services.eventbridge.model.EventBridgeException;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequest;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequestEntry;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import static it.pagopa.pnss.common.utils.LogUtils.*;
import static it.pagopa.pnss.common.utils.ReactorUtils.pullFromFluxUntilIsEmpty;

@CustomLog
@Service("streamsRecordProcessor")
public class StreamsRecordProcessor {


    public static final String INSERT_EVENT = "INSERT";
    public static final String MODIFY_EVENT = "MODIFY";
    public static final String REMOVE_EVENT = "REMOVE";
    private static final String CAN_READ_TAGS = "canReadTags";
    private final EventBridgeClient eventBridgeClient = EventBridgeClient.create();
    private final SqsService sqsService;
    private final StreamRecordProcessorQueueName streamRecordProcessorQueueName;

    private final RetryBackoffSpec sqsEnventHandlerRetryStrategy;

    @Value("${pn.ss.event-handler.max.messages}")
    private int maxMessages;


    DynamoDbAsyncClient dynamoDbClient;
    @Value("${event.bridge.disponibilita-documenti-name}")
    private String disponibilitaDocumentiEventBridge;
    @Value("$(pn.ss.safe-clients)")
    private String safeClients;




    public StreamsRecordProcessor(DynamoDbAsyncClient dynamoDbClient, SqsService sqsService, StreamRecordProcessorQueueName streamRecordProcessorQueueName, SqsEventHandlerRetryStrategyProperties sqsEventHandlerRetryStrategyProperties) {

        this.dynamoDbClient = dynamoDbClient;
        this.sqsService = sqsService;
        this.streamRecordProcessorQueueName = streamRecordProcessorQueueName;
        this.sqsEnventHandlerRetryStrategy = Retry.backoff(sqsEventHandlerRetryStrategyProperties.maxAttempts(), Duration.ofSeconds(sqsEventHandlerRetryStrategyProperties.minBackoff()))
                .filter(throwable -> throwable instanceof DynamoDbException || throwable instanceof SdkClientException || throwable instanceof EventBridgeException)
                .doBeforeRetry(retrySignal -> log.warn(RETRY_ATTEMPT, retrySignal.totalRetries(), retrySignal.failure()+","+retrySignal.failure().getMessage()));
    }

    @Scheduled(cron="${PnSsCronStreamsRecordProcessor ?:*/10 * * * * *}")
    public void processRecords() {
        final String PROCESS_RECORDS = "processRecords()";
        MDC.clear();
        log.logStartingProcess(PROCESS_RECORDS);

        findEventSendToBridge()
                .buffer(10)
                .map(tuple -> {
                    List<PutEventsRequestEntry> putEventsRequestEntries = tuple.stream()
                            .map(Tuple2::getT2)
                            .toList();
                    PutEventsRequest eventsRequest = PutEventsRequest.builder()
                            .entries(putEventsRequestEntries)
                            .build();

                    log.debug(CLIENT_METHOD_INVOCATION, "eventBridgeClient.putEvents()", eventsRequest);

                    eventBridgeClient.putEvents(eventsRequest);

                    return tuple.stream()
                            .map(Tuple2::getT1)
                            .toList();
                })
                .flatMap(wrappers ->
                     Flux.fromIterable(wrappers)
                            .map(SqsMessageWrapper::getMessage)
                            .flatMap(message ->sqsService.deleteMessageFromQueue(message, streamRecordProcessorQueueName.sqsName()))
                )
                .retryWhen(sqsEnventHandlerRetryStrategy)
                .transform(pullFromFluxUntilIsEmpty())
                .then()
                .doOnError(e -> log.fatal("DBStream: Errore generico ", e))
                .doOnSuccess(unused -> log.logEndingProcess(PROCESS_RECORDS))
                .subscribe();
    }


    public Flux<Tuple2<SqsMessageWrapper<DocumentStateDto>, PutEventsRequestEntry>> findEventSendToBridge() {
        final String FIND_EVENT_SEND_TO_BRIDGE = "StreamRecordProcessor.findEventSendToBridge()";
        log.debug(INVOKING_METHOD, FIND_EVENT_SEND_TO_BRIDGE);
        return sqsService.getMessages(streamRecordProcessorQueueName.sqsName(), DocumentStateDto.class, maxMessages)
                .flatMap(recordEvent -> {
                    DocumentEntity docEntity = recordEvent.getMessageContent().getDocumentEntity();
                    MDC.put(MDC_CORR_ID_KEY, docEntity.getDocumentKey());
                    return MDCUtils.addMDCToContextAndExecute(getCanReadTags(docEntity)
                            .mapNotNull(canReadTags -> {
                                PutEventsRequestEntry putEventsRequestEntry = EventBridgeUtil.createMessage(docEntity,
                                        disponibilitaDocumentiEventBridge,
                                        docEntity.getDocumentState(),
                                        canReadTags);
                                if (putEventsRequestEntry != null) {
                                    log.info("Event send to bridge {}", putEventsRequestEntry);
                                }
                                return Tuples.of(recordEvent, Objects.requireNonNull(putEventsRequestEntry));
                            }));
                })
                .doOnError(e -> log.fatal("DBStream: Errore generico nella gestione dell'evento - {}", e.getMessage(), e))
                .doOnComplete(() -> log.info("DBStream: Nessun evento da inviare a bridge"));
    }


    public Mono<Boolean> getCanReadTags(DocumentEntity docEntity) {
        final String METHOD_NAME = "getCanReadTags()";
        log.debug(LogUtils.INVOKING_METHOD, METHOD_NAME, Stream.of(docEntity).toList());
        String cxId = docEntity.getClientShortCode();
        return Mono.fromSupplier(() -> {
                    boolean hasTags = hasTags(docEntity);
                    boolean isClientInList = isClientInList(cxId);
                    boolean isCheckDisabled = isCheckDisabled();
                    if (!hasTags) {
                        log.debug("DBStream: Nessun tag presente nel record");
                    }
                    return ((isClientInList || isCheckDisabled) && hasTags) || (!isClientInList && !isCheckDisabled);
                })
                .filter(Boolean::booleanValue)
                .flatMap(unused -> Mono.fromCompletionStage(getFromDynamo(cxId)))
                .retryWhen(sqsEnventHandlerRetryStrategy)
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
        if (safeClients == null || safeClients.isEmpty()) {
            safeClients="";
        }
        return List.of(safeClients.strip().split(";"));
    }

    private boolean isCheckDisabled() {
        List<String> clientList = getClientList();
        return clientList != null && clientList.size() == 1 && "DISABLED".equals(clientList.get(0));
    }


    private boolean hasTags(DocumentEntity inputRecord) {
        return inputRecord.getTags() != null && !inputRecord.getTags().isEmpty();
    }



}