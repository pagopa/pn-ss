package it.pagopa.pnss.availabledocument;

import com.amazonaws.services.dynamodbv2.streamsadapter.adapter.DynamoDBStreamsClientRecord;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.Record;
import software.amazon.awssdk.services.dynamodb.model.StreamRecord;
import software.amazon.kinesis.lifecycle.events.ProcessRecordsInput;
import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.UserConfiguration;
import it.pagopa.pnss.availabledocument.dto.NotificationMessage;
import it.pagopa.pnss.availabledocument.event.StreamsRecordProcessor;
import it.pagopa.pnss.common.DocTypesConstant;
import it.pagopa.pnss.common.model.dto.DocumentStateDto;
import it.pagopa.pnss.common.model.pojo.SqsMessageWrapper;
import it.pagopa.pnss.common.service.SqsService;
import it.pagopa.pnss.configurationproperties.AvailabelDocumentEventBridgeName;
import it.pagopa.pnss.configurationproperties.StreamRecordProcessorQueueName;
import it.pagopa.pnss.repositorymanager.entity.DocTypeEntity;
import it.pagopa.pnss.repositorymanager.entity.DocumentEntity;
import it.pagopa.pnss.testutils.annotation.SpringBootTestWebEnv;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.core.env.Environment;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import reactor.util.function.Tuple2;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequestEntry;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;
import software.amazon.kinesis.processor.RecordProcessorCheckpointer;
import software.amazon.kinesis.retrieval.KinesisClientRecord;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static it.pagopa.pnss.availabledocument.event.StreamsRecordProcessor.*;
import static it.pagopa.pnss.common.constant.Constant.*;
import static it.pagopa.pnss.common.utils.EventBridgeUtil.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.http.MediaType.APPLICATION_PDF_VALUE;

@SpringBootTestWebEnv
@AutoConfigureWebTestClient
class StreamsRecordProcessorTest {

    @Autowired
    DynamoDbAsyncClient dynamoDbAsyncClient;
    @Autowired
    DynamoDbClient dynamoDbClient;
    @Autowired
    AvailabelDocumentEventBridgeName availabelDocumentEventBridgeName;
    @Autowired
    Environment environment;
    @Autowired
    SqsService sqsService;
    @Autowired
    SqsAsyncClient sqsAsyncClient;
    @Autowired
    StreamsRecordProcessor srp;
    @Autowired
    private  StreamRecordProcessorQueueName streamRecordProcessorQueueName;

    private static final String AUTHORIZED_CLIENT = "pn-delivery";
    private static final String UNAUTHORIZED_CLIENT = "pn-delivery-unauthorized";
    private static final String CHECK_DISABLED = "DISABLED";

    @BeforeEach
    void setUp() {
        putAnagraficaClient(createClient(AUTHORIZED_CLIENT));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false})
    void testProcessRecordsWithoutPermissions(Boolean canReadTagsValue)  {
        sendMessageToQueue().block();

        UserConfiguration client = createClient(AUTHORIZED_CLIENT);
        client.setCanReadTags(canReadTagsValue);
        putAnagraficaClient(client);

        //ProcessRecordsInput processRecordsInput = new ProcessRecordsInput();
        //List<Record> records = new ArrayList<>();
        //records.add(new RecordAdapter(recordDyanmo));
        //processRecordsInput.withRecords(records);

        List<KinesisClientRecord> records = new ArrayList<>();
        Record recordDynamo = createRecorDynamo(MODIFY_EVENT,AVAILABLE,BOOKED,true,AUTHORIZED_CLIENT);
        records.add(DynamoDBStreamsClientRecord.fromRecord((recordDynamo)));

        ProcessRecordsInput processRecordsInput = ProcessRecordsInput.builder()
                .records(records)
                .checkpointer(mock(RecordProcessorCheckpointer.class))
                .millisBehindLatest(0L)
                .build();

        Flux<Tuple2<SqsMessageWrapper<DocumentStateDto>, PutEventsRequestEntry>> eventSendToBridge = srp.findEventSendToBridge();
        StepVerifier.create(eventSendToBridge).assertNext(putEventsRequestEntry -> {
            NotificationMessage notificationMessage = eventToNotificationMessage(putEventsRequestEntry.getT2().detail());
            Assertions.assertNull(notificationMessage.getTags());
        }).verifyComplete();
        System.out.println("response: "+eventSendToBridge);
    }



    @Test
    void testProcessRecordsWithPermissions(){
        sendMessageToQueue().block();

        //ProcessRecordsInput processRecordsInput = new ProcessRecordsInput();
        //List<Record> records = new ArrayList<>();

        List<KinesisClientRecord> records = new ArrayList<>();
        Record recordDynamo = createRecorDynamo(MODIFY_EVENT,AVAILABLE,BOOKED,true,AUTHORIZED_CLIENT);
        records.add(DynamoDBStreamsClientRecord.fromRecord((recordDynamo)));

        //records.add(new RecordAdapter(recordDyanmo));
        //processRecordsInput.withRecords(records);
        ProcessRecordsInput processRecordsInput = ProcessRecordsInput.builder()
                .records(records)
                .checkpointer(mock(RecordProcessorCheckpointer.class))
                .millisBehindLatest(0L)
                .build();

        Flux<Tuple2<SqsMessageWrapper<DocumentStateDto>, PutEventsRequestEntry>> eventSendToBridge = srp.findEventSendToBridge();
        StepVerifier.create(eventSendToBridge).assertNext(putEventsRequestEntry -> {
            NotificationMessage notificationMessage = eventToNotificationMessage(putEventsRequestEntry.getT2().detail());
            Assertions.assertLinesMatch(List.of("value1","value2"),notificationMessage.getTags().get("tag1"));
        }).verifyComplete();

    }


    @Test
    void testProcessRecordsOk() {

        //ProcessRecordsInput processRecordsInput = new ProcessRecordsInput();
        List<KinesisClientRecord> records = new ArrayList<>();

        Record recordDyanmo = createRecorDynamo(MODIFY_EVENT, AVAILABLE, BOOKED,true,AUTHORIZED_CLIENT);
        records.add(DynamoDBStreamsClientRecord.fromRecord((recordDyanmo)));
        //records.add(new RecordAdapter(recordDyanmo));
        //processRecordsInput.withRecords(records);
        ProcessRecordsInput processRecordsInput = ProcessRecordsInput.builder()
                .records(records)
                .checkpointer(mock(RecordProcessorCheckpointer.class))
                .millisBehindLatest(0L)
                .build();

        Assertions.assertDoesNotThrow(srp::processRecords);
    }

    @ParameterizedTest
    @MethodSource("provideClientsAndTagsParameters")
    void testProcessRecordsWithDifferentConditions(String clientName, boolean withTags, int expectedCount) {
        sendMessageToQueue().block();
        UserConfiguration client = createClient(clientName);
        putAnagraficaClient(client);

        //ProcessRecordsInput processRecordsInput = new ProcessRecordsInput();
        List<KinesisClientRecord> records = new ArrayList<>();

        Record recordDynamo = createRecorDynamo(MODIFY_EVENT,AVAILABLE,BOOKED,withTags,clientName);
        records.add(DynamoDBStreamsClientRecord.fromRecord((recordDynamo)));

        //records.add(new RecordAdapter(recordDyanmo));
        //processRecordsInput.withRecords(records);

        ProcessRecordsInput processRecordsInput = ProcessRecordsInput.builder()
                .records(records)
                .checkpointer(mock(RecordProcessorCheckpointer.class))
                .millisBehindLatest(0L)
                .build();

        Flux<Tuple2<SqsMessageWrapper<DocumentStateDto>, PutEventsRequestEntry>> eventSendToBridge = srp.findEventSendToBridge();
        StepVerifier.create(eventSendToBridge).expectNextCount(expectedCount).verifyComplete();
    }



    @ParameterizedTest
    @MethodSource("provideClientsAndTagsParametersWithClientsDisabled")
    void testProcessRecordsWithCheckDisabled(String clientName, boolean withTags, int expectedCount) {
        sendMessageToQueue().block();

        System.setProperty("pn.ss.safe-clients",CHECK_DISABLED);

        UserConfiguration client = createClient(clientName);
        putAnagraficaClient(client);

        //ProcessRecordsInput processRecordsInput = new ProcessRecordsInput();
        List<KinesisClientRecord> records = new ArrayList<>();

        Record recordDynamo = createRecorDynamo(MODIFY_EVENT,AVAILABLE,BOOKED,withTags,clientName);
        records.add(DynamoDBStreamsClientRecord.fromRecord((recordDynamo)));

        //records.add(new RecordAdapter(recordDyanmo));
        //processRecordsInput.withRecords(records);
        ProcessRecordsInput processRecordsInput = ProcessRecordsInput.builder()
                .records(records)
                .checkpointer(mock(RecordProcessorCheckpointer.class))
                .millisBehindLatest(0L)
                .build();

        Flux<Tuple2<SqsMessageWrapper<DocumentStateDto>, PutEventsRequestEntry>> eventSendToBridge = srp.findEventSendToBridge();
        StepVerifier.create(eventSendToBridge).expectNextCount(expectedCount).verifyComplete();
    }




    @Test
    void testProcessRecordsWithDynamoDbError() {
        sendMessageToQueue().block();

        //ProcessRecordsInput processRecordsInput = new ProcessRecordsInput();
        List<KinesisClientRecord> records = new ArrayList<>();


        Record recordDyanmo = createRecorDynamo(MODIFY_EVENT,AVAILABLE,BOOKED,true,AUTHORIZED_CLIENT);
        DocumentEntity documentEntity = new DocumentEntity();
        documentEntity.setDocumentKey("111");
        documentEntity.setDocumentState("AVAILABLE");
        documentEntity.setDocumentLogicalState("BOOKED");
        documentEntity.setDocumentType(new DocTypeEntity(){{
            setTipoDocumento(DocTypesConstant.PN_NOTIFICATION_ATTACHMENTS);
            setChecksum("MD5");
        }});

        StreamsRecordProcessor srpSpy = spy(srp);
        doThrow(SdkClientException.class).when(srpSpy).getFromDynamo(anyString());
        doCallRealMethod().when(srpSpy).getCanReadTags(documentEntity);
        records.add(DynamoDBStreamsClientRecord.fromRecord((recordDyanmo)));
//        records.add(new RecordAdapter(recordDyanmo));
//        processRecordsInput.withRecords(records);
        ProcessRecordsInput processRecordsInput = ProcessRecordsInput.builder()
                .records(records)
                .checkpointer(mock(RecordProcessorCheckpointer.class))
                .millisBehindLatest(0L)
                .build();

        Flux<Tuple2<SqsMessageWrapper<DocumentStateDto>, PutEventsRequestEntry>> eventSendToBridge = srpSpy.findEventSendToBridge();

        StepVerifier.create(eventSendToBridge)
                .expectError(IllegalStateException.class)
                .verify();
    }



    @Test
    void testSendMessageEventBridgeOk() {
        sendMessageToQueue().block();

        //ProcessRecordsInput processRecordsInput = new ProcessRecordsInput();
        List<KinesisClientRecord> records = new ArrayList<>();

        Record recordDyanmo = createRecorDynamo(MODIFY_EVENT,AVAILABLE,BOOKED,true,AUTHORIZED_CLIENT);
        records.add(DynamoDBStreamsClientRecord.fromRecord((recordDyanmo)));
        //records.add(new RecordAdapter(recordDyanmo));
        //processRecordsInput.withRecords(records);
        ProcessRecordsInput processRecordsInput = ProcessRecordsInput.builder()
                .records(records)
                .checkpointer(mock(RecordProcessorCheckpointer.class))
                .millisBehindLatest(0L)
                .build();
        Flux<Tuple2<SqsMessageWrapper<DocumentStateDto>, PutEventsRequestEntry>> eventSendToBridge = srp.findEventSendToBridge();
        StepVerifier.create(eventSendToBridge).expectNextCount(1).verifyComplete();
    }



    @Test
    void testSendMessageEventBridgeInsert() {

        //ProcessRecordsInput processRecordsInput = new ProcessRecordsInput();
        List<KinesisClientRecord> records = new ArrayList<>();

        Record recordDyanmo = createRecorDynamo(INSERT_EVENT,AVAILABLE,BOOKED,true,AUTHORIZED_CLIENT);
        records.add(DynamoDBStreamsClientRecord.fromRecord((recordDyanmo)));

        //records.add(new RecordAdapter(recordDyanmo));
        //processRecordsInput.withRecords(records);
        ProcessRecordsInput processRecordsInput = ProcessRecordsInput.builder()
                .records(records)
                .checkpointer(mock(RecordProcessorCheckpointer.class))
                .millisBehindLatest(0L)
                .build();

        Flux<Tuple2<SqsMessageWrapper<DocumentStateDto>, PutEventsRequestEntry>> eventSendToBridge = srp.findEventSendToBridge();
        StepVerifier.create(eventSendToBridge).expectNextCount(0).verifyComplete();
    }

    @Test
    void testSendMessageEventBridgeDelete(){

        //ProcessRecordsInput processRecordsInput = new ProcessRecordsInput();
        List<KinesisClientRecord> records = new ArrayList<>();

        Record recordDyanmo = createRecorDynamo(REMOVE_EVENT,AVAILABLE,BOOKED,true,AUTHORIZED_CLIENT);
        records.add(DynamoDBStreamsClientRecord.fromRecord((recordDyanmo)));
        //records.add(new RecordAdapter(recordDyanmo));
        //processRecordsInput.withRecords(records);
        ProcessRecordsInput processRecordsInput = ProcessRecordsInput.builder()
                .records(records)
                .checkpointer(mock(RecordProcessorCheckpointer.class))
                .millisBehindLatest(0L)
                .build();

        Flux<Tuple2<SqsMessageWrapper<DocumentStateDto>, PutEventsRequestEntry>> eventSendToBridge = srp.findEventSendToBridge();
        StepVerifier.create(eventSendToBridge).expectNextCount(0).verifyComplete();
    }

    @Test
    void testSendMessageEventOldNewSameState(){

        //ProcessRecordsInput processRecordsInput = new ProcessRecordsInput();
        List<KinesisClientRecord> records = new ArrayList<>();

        Record recordDyanmo = createRecorDynamo(MODIFY_EVENT,AVAILABLE,AVAILABLE,true,AUTHORIZED_CLIENT);
        records.add(DynamoDBStreamsClientRecord.fromRecord((recordDyanmo)));
        //records.add(new RecordAdapter(recordDyanmo));
        //processRecordsInput.withRecords(records);
        ProcessRecordsInput processRecordsInput = ProcessRecordsInput.builder()
                .records(records)
                .checkpointer(mock(RecordProcessorCheckpointer.class))
                .millisBehindLatest(0L)
                .build();

        Flux<Tuple2<SqsMessageWrapper<DocumentStateDto>, PutEventsRequestEntry>> eventSendToBridge = srp.findEventSendToBridge();
        StepVerifier.create(eventSendToBridge).expectNextCount(0).verifyComplete();
    }

    @NotNull
    private  Record createRecorDynamo(String eventName ,String documentStateNew,  String documentStateOld, boolean wTags,String clientName) {

        // costruisci prima la mappa documentType completa
        Map<String, AttributeValue> docTypeMapNew = new HashMap<>();
        docTypeMapNew.put(TIPODOCUMENTO_KEY, createAttributeS(DocTypesConstant.PN_NOTIFICATION_ATTACHMENTS));
        docTypeMapNew.put(CHECKSUM_KEY, createAttributeS("MD5"));

        Map<String, AttributeValue> docTypeMapOld = new HashMap<>();
        docTypeMapOld.put(TIPODOCUMENTO_KEY, createAttributeS(DocTypesConstant.PN_NOTIFICATION_ATTACHMENTS));
        docTypeMapOld.put(CHECKSUM_KEY, createAttributeS("MD5"));

        Map<String,AttributeValue> tags = new HashMap<>();
        if(wTags){
            tags.put("tag1", AttributeValue.builder()
                    .l( AttributeValue.builder().s("value1").build(),
                            AttributeValue.builder().s("value2").build())
                    .build());
        }

        Map<String, AttributeValue> image = new HashMap<>();
        if (wTags) {
            image.put(TAGS_KEY, AttributeValue.builder().m(tags).build());
        }
        image.put(DOCUMENTKEY_KEY, createAttributeS("111"));
        image.put(DOCUMENTSTATE_KEY, createAttributeS(documentStateNew));
        image.put(DOCUMENTTYPE_KEY, AttributeValue.builder().m(docTypeMapNew).build());

        //image.put(DOCUMENTTYPE_KEY, createAttributeM());
        //image.get(DOCUMENTTYPE_KEY).m().put(TIPODOCUMENTO_KEY,createAttributeS(DocTypesConstant.PN_NOTIFICATION_ATTACHMENTS));
        image.put(DOCUMENTLOGICALSTATE_KEY, createAttributeS(documentStateOld));
        image.put(CONTENTTYPE_KEY, createAttributeS(APPLICATION_PDF_VALUE));
        //image.get(DOCUMENTTYPE_KEY).m().put(CHECKSUM_KEY,createAttributeS("MD5"));
        image.put(RETENTIONUNTIL_KEY, createAttributeS("80"));
        image.put(CLIENTSHORTCODE_KEY, createAttributeS(createClient(clientName).getName()));
        //dynamodbRecord.setNewImage(image);

        Map<String, AttributeValue> oldImage = new HashMap<>();
        oldImage.put(DOCUMENTKEY_KEY, createAttributeS("111"));
        oldImage.put(DOCUMENTSTATE_KEY, createAttributeS(documentStateOld));
        //oldImage.put(DOCUMENTTYPE_KEY, createAttributeM());
        //oldImage.get(DOCUMENTTYPE_KEY).m().put(TIPODOCUMENTO_KEY,createAttributeS(DocTypesConstant.PN_NOTIFICATION_ATTACHMENTS));
        oldImage.put(DOCUMENTTYPE_KEY, AttributeValue.builder().m(docTypeMapOld).build());
        oldImage.put(DOCUMENTLOGICALSTATE_KEY, createAttributeS(SAVED));
        oldImage.put(CONTENTTYPE_KEY, createAttributeS(APPLICATION_PDF_VALUE));
        //oldImage.get(DOCUMENTTYPE_KEY).m().put(CHECKSUM_KEY,createAttributeS("MD5"));
        oldImage.put(RETENTIONUNTIL_KEY, createAttributeS("80"));
        oldImage.put(CLIENTSHORTCODE_KEY, createAttributeS(createClient(clientName).getName()));
        if (wTags)  oldImage.put(TAGS_KEY, AttributeValue.fromM(createAttributeM().m()));

        //dynamodbRecord.setOldImage(image);

        //recordDyanmo.setDynamodb(dynamodbRecord);
        StreamRecord dynamodbRecord = StreamRecord.builder()
                .newImage(image)
                .oldImage(oldImage)
                .build();

        //return recordDyanmo;
        return Record.builder()
                .eventName(eventName)
                .dynamodb(dynamodbRecord)
                .build();

    }

    private void putAnagraficaClient(UserConfiguration client) {
        var itemMap = new HashMap<>(Map.of("name", software.amazon.awssdk.services.dynamodb.model.AttributeValue.builder().s(client.getName()).build(),
                "canWriteTags", software.amazon.awssdk.services.dynamodb.model.AttributeValue.builder().bool(client.getCanWriteTags()).build(),
                "canExecutePatch", software.amazon.awssdk.services.dynamodb.model.AttributeValue.builder().bool(client.getCanExecutePatch()).build(),
                "apiKey", software.amazon.awssdk.services.dynamodb.model.AttributeValue.builder().s(client.getApiKey()).build()));
        if (client.getCanReadTags() != null) {
            itemMap.put("canReadTags", software.amazon.awssdk.services.dynamodb.model.AttributeValue.builder().bool(client.getCanReadTags()).build());
        }
        dynamoDbClient.putItem(request -> request.tableName("pn-SsAnagraficaClient").item(itemMap));
    }

    private static UserConfiguration createClient(String clientName) {
        UserConfiguration client = new UserConfiguration();
        client.setName(clientName);
        client.setCanReadTags(true);
        client.setCanWriteTags(true);
        client.setCanExecutePatch(true);
        client.setApiKey("apiKey");

        return client;
    }

    private NotificationMessage eventToNotificationMessage(String event) {
        ObjectMapper objMapper = new ObjectMapper();
        try {
            return objMapper.readValue(event, NotificationMessage.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private  AttributeValue createAttributeM() {
//        AttributeValue toRet = new AttributeValue();
//        Map<String, AttributeValue> map = new HashMap<>();
//        toRet.setM(map);
//        return toRet;
        return AttributeValue.builder().m(new HashMap<>()).build();
    }

    private static Stream<Arguments> provideClientsAndTagsParameters() {
        return Stream.of(
                Arguments.of(UNAUTHORIZED_CLIENT, true,1), // unauthorized client with tags returns 1 event
                Arguments.of(UNAUTHORIZED_CLIENT, false,1), // unauthorized client without tags returns 1 event
                Arguments.of(AUTHORIZED_CLIENT, true,1), // authorized client with tags returns 1 event
                Arguments.of(AUTHORIZED_CLIENT, false,1) // authorized client without tags returns 1 event

        );
    }

    public static Stream<Arguments> provideClientsAndTagsParametersWithClientsDisabled() {
        return Stream.of(
                Arguments.of(UNAUTHORIZED_CLIENT, true,1), // unauthorized client with tags returns 1 event
                Arguments.of(UNAUTHORIZED_CLIENT, false,1), // unauthorized client without tags returns 1 event
                Arguments.of(AUTHORIZED_CLIENT, true,1), // authorized client with tags returns 1 event
                Arguments.of(AUTHORIZED_CLIENT, false,1) // authorized client without tags returns 1 event
        );
    }

    @NotNull
    private static AttributeValue createAttributeS(String sVAlue) {
//        AttributeValue value = new AttributeValue();
//        value.setS(sVAlue);
//        return value;
        return AttributeValue.builder().s(sVAlue).build();
    }


    @BeforeEach
    void setup() {
        sqsAsyncClient.purgeQueue(builder -> builder.queueUrl(streamRecordProcessorQueueName.sqsName()));
    }

    private Mono<SendMessageResponse> sendMessageToQueue() {
        return sqsService.send(streamRecordProcessorQueueName.sqsName(), new DocumentStateDto(){{
            setDocumentEntity(new DocumentEntity(){{
                setDocumentKey("111");
                setDocumentState("AVAILABLE");
                setDocumentLogicalState("BOOKED");
                setClientShortCode("pn-delivery");
                setTags(Map.of("tag1", List.of("value1", "value2")));
                setDocumentType(new DocTypeEntity(){{
                    setTipoDocumento(DocTypesConstant.PN_NOTIFICATION_ATTACHMENTS);
                    setChecksum("MD5");
                }});
            }});
            setOldDocumentState("BOOKED");
        }});
    }


}

