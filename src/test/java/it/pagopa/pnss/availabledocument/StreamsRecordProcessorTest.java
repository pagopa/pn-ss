package it.pagopa.pnss.availabledocument;

import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.StreamRecord;
import com.amazonaws.services.dynamodbv2.streamsadapter.model.RecordAdapter;
import com.amazonaws.services.kinesis.clientlibrary.types.ProcessRecordsInput;
import com.amazonaws.services.kinesis.model.Record;
import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.UserConfiguration;
import it.pagopa.pnss.availabledocument.dto.NotificationMessage;
import it.pagopa.pnss.availabledocument.event.StreamsRecordProcessor;
import it.pagopa.pnss.common.DocTypesConstant;
import it.pagopa.pnss.configurationproperties.AvailabelDocumentEventBridgeName;
import it.pagopa.pnss.testutils.annotation.SpringBootTestWebEnv;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.core.env.Environment;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequestEntry;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static it.pagopa.pnss.common.utils.EventBridgeUtil.*;
import static it.pagopa.pnss.availabledocument.event.StreamsRecordProcessor.*;
import static it.pagopa.pnss.common.constant.Constant.*;
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
    private static final String AUTHORIZED_CLIENT = "pn-delivery";
    private static final String UNAUTHORIZED_CLIENT = "pn-delivery-unauthorized";
    private static final String CHECK_DISABLED = "DISABLED";

    @BeforeEach
    void setUp() {
        putAnagraficaClient(createClient(AUTHORIZED_CLIENT));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(booleans = {false})
    void testProcessRecordsWithoutPermissions(Boolean canReadTagsValue)  {
        StreamsRecordProcessor srp = new StreamsRecordProcessor(availabelDocumentEventBridgeName.disponibilitaDocumentiName(), dynamoDbAsyncClient,true);

        UserConfiguration client = createClient(AUTHORIZED_CLIENT);
        client.setCanReadTags(canReadTagsValue);
        putAnagraficaClient(client);

        ProcessRecordsInput processRecordsInput = new ProcessRecordsInput();
        List<Record> records = new ArrayList<>();

        com.amazonaws.services.dynamodbv2.model.Record recordDyanmo = createRecorDynamo(MODIFY_EVENT,AVAILABLE,BOOKED,true,AUTHORIZED_CLIENT);

        records.add(new RecordAdapter(recordDyanmo));
        processRecordsInput.withRecords(records);
        Flux<PutEventsRequestEntry> eventSendToBridge = srp.findEventSendToBridge(processRecordsInput);
        StepVerifier.create(eventSendToBridge).assertNext(putEventsRequestEntry -> {
            NotificationMessage notificationMessage = eventToNotificationMessage(putEventsRequestEntry.detail());
            Assertions.assertNull(notificationMessage.getTags());
        }).verifyComplete();
        System.out.println("response: "+eventSendToBridge.toString());
    }

    @Test
    void testProcessRecordsWithPermissions(){
        StreamsRecordProcessor srp = new StreamsRecordProcessor(availabelDocumentEventBridgeName.disponibilitaDocumentiName(), dynamoDbAsyncClient,true);

        ProcessRecordsInput processRecordsInput = new ProcessRecordsInput();
        List<Record> records = new ArrayList<>();

        com.amazonaws.services.dynamodbv2.model.Record recordDyanmo = createRecorDynamo(MODIFY_EVENT,AVAILABLE,BOOKED,true,AUTHORIZED_CLIENT);

        records.add(new RecordAdapter(recordDyanmo));
        processRecordsInput.withRecords(records);
        Flux<PutEventsRequestEntry> eventSendToBridge = srp.findEventSendToBridge(processRecordsInput);
        StepVerifier.create(eventSendToBridge).assertNext(putEventsRequestEntry -> {
            NotificationMessage notificationMessage = eventToNotificationMessage(putEventsRequestEntry.detail());
            Assertions.assertLinesMatch(List.of("value1","value2"),notificationMessage.getTags().get("tag1"));
        }).verifyComplete();

    }
    @Test
    void testProcessRecordsOk() {
        StreamsRecordProcessor srp = new StreamsRecordProcessor(availabelDocumentEventBridgeName.disponibilitaDocumentiName(), dynamoDbAsyncClient,true);

        ProcessRecordsInput processRecordsInput = new ProcessRecordsInput();
        List<Record> records = new ArrayList<>();

        com.amazonaws.services.dynamodbv2.model.Record recordDyanmo = createRecorDynamo(MODIFY_EVENT, AVAILABLE, BOOKED,true,AUTHORIZED_CLIENT);

        records.add(new RecordAdapter(recordDyanmo));
        processRecordsInput.withRecords(records);

        Assertions.assertDoesNotThrow(() -> srp.processRecords(processRecordsInput));
    }
    @ParameterizedTest
    @MethodSource("provideClientsAndTagsParameters")
    void testProcessRecordsWithDifferentConditions(String clientName, boolean withTags, int expectedCount) {
        StreamsRecordProcessor srp = new StreamsRecordProcessor(availabelDocumentEventBridgeName.disponibilitaDocumentiName(), dynamoDbAsyncClient,true);

        UserConfiguration client = createClient(clientName);
        putAnagraficaClient(client);

        ProcessRecordsInput processRecordsInput = new ProcessRecordsInput();
        List<Record> records = new ArrayList<>();

        com.amazonaws.services.dynamodbv2.model.Record recordDyanmo = createRecorDynamo(MODIFY_EVENT,AVAILABLE,BOOKED,withTags,clientName);


        records.add(new RecordAdapter(recordDyanmo));
        processRecordsInput.withRecords(records);
        Flux<PutEventsRequestEntry> eventSendToBridge = srp.findEventSendToBridge(processRecordsInput);
        StepVerifier.create(eventSendToBridge).expectNextCount(expectedCount).verifyComplete();
    }

    @ParameterizedTest
    @MethodSource("provideClientsAndTagsParametersWithClientsDisabled")
    void testProcessRecordsWithCheckDisabled(String clientName, boolean withTags, int expectedCount) {
        System.setProperty("pn.ss.safe-clients",CHECK_DISABLED);
        StreamsRecordProcessor srp = new StreamsRecordProcessor(availabelDocumentEventBridgeName.disponibilitaDocumentiName(), dynamoDbAsyncClient,true);

        UserConfiguration client = createClient(clientName);
        putAnagraficaClient(client);

        ProcessRecordsInput processRecordsInput = new ProcessRecordsInput();
        List<Record> records = new ArrayList<>();

        com.amazonaws.services.dynamodbv2.model.Record recordDyanmo = createRecorDynamo(MODIFY_EVENT,AVAILABLE,BOOKED,withTags,clientName);


        records.add(new RecordAdapter(recordDyanmo));
        processRecordsInput.withRecords(records);
        Flux<PutEventsRequestEntry> eventSendToBridge = srp.findEventSendToBridge(processRecordsInput);
        StepVerifier.create(eventSendToBridge).expectNextCount(expectedCount).verifyComplete();
    }


    @Test
    void testProcessRecordsWithDynamoDbError() {
        StreamsRecordProcessor srp = new StreamsRecordProcessor(availabelDocumentEventBridgeName.disponibilitaDocumentiName(), dynamoDbAsyncClient,true);

        ProcessRecordsInput processRecordsInput = new ProcessRecordsInput();
        List<Record> records = new ArrayList<>();


        com.amazonaws.services.dynamodbv2.model.Record recordDyanmo = createRecorDynamo(MODIFY_EVENT,AVAILABLE,BOOKED,true,AUTHORIZED_CLIENT);
        StreamsRecordProcessor srpSpy = spy(srp);
        doThrow(SdkClientException.class).when(srpSpy).getFromDynamo(anyString());
        doCallRealMethod().when(srpSpy).getCanReadTags(recordDyanmo);
        records.add(new RecordAdapter(recordDyanmo));
        processRecordsInput.withRecords(records);
        Flux<PutEventsRequestEntry> eventSendToBridge = srpSpy.findEventSendToBridge(processRecordsInput);

        StepVerifier.create(eventSendToBridge)
                .thenAwait(Duration.ofSeconds(25))
                .expectTimeout(Duration.ofSeconds(24))
                .verify();
    }

    @Test
    void testSendMessageEventBridgeOk() {
        StreamsRecordProcessor srp = new StreamsRecordProcessor(availabelDocumentEventBridgeName.disponibilitaDocumentiName(), dynamoDbAsyncClient,true);

        ProcessRecordsInput processRecordsInput = new ProcessRecordsInput();
        List<Record> records = new ArrayList<>();

        com.amazonaws.services.dynamodbv2.model.Record recordDyanmo = createRecorDynamo(MODIFY_EVENT,AVAILABLE,BOOKED,true,AUTHORIZED_CLIENT);

        records.add(new RecordAdapter(recordDyanmo));
        processRecordsInput.withRecords(records);
        Flux<PutEventsRequestEntry> eventSendToBridge = srp.findEventSendToBridge(processRecordsInput);
        StepVerifier.create(eventSendToBridge).expectNextCount(1).verifyComplete();
    }

    @Test
    void testSendMessageEventBridgeInsert() {
        StreamsRecordProcessor srp = new StreamsRecordProcessor(availabelDocumentEventBridgeName.disponibilitaDocumentiName(), dynamoDbAsyncClient, true);

        ProcessRecordsInput processRecordsInput = new ProcessRecordsInput();
        List<Record> records = new ArrayList<>();

        com.amazonaws.services.dynamodbv2.model.Record recordDyanmo = createRecorDynamo(INSERT_EVENT,AVAILABLE,BOOKED,true,AUTHORIZED_CLIENT);

        records.add(new RecordAdapter(recordDyanmo));
        processRecordsInput.withRecords(records);
        Flux<PutEventsRequestEntry> eventSendToBridge = srp.findEventSendToBridge(processRecordsInput);
        StepVerifier.create(eventSendToBridge).expectNextCount(0).verifyComplete();
    }

    @Test
    void testSendMessageEventBridgeDelete(){
        StreamsRecordProcessor srp = new StreamsRecordProcessor(availabelDocumentEventBridgeName.disponibilitaDocumentiName(), dynamoDbAsyncClient,true);

        ProcessRecordsInput processRecordsInput = new ProcessRecordsInput();
        List<Record> records = new ArrayList<>();

        com.amazonaws.services.dynamodbv2.model.Record recordDyanmo = createRecorDynamo(REMOVE_EVENT,AVAILABLE,BOOKED,true,AUTHORIZED_CLIENT);

        records.add(new RecordAdapter(recordDyanmo));
        processRecordsInput.withRecords(records);
        Flux<PutEventsRequestEntry> eventSendToBridge = srp.findEventSendToBridge(processRecordsInput);
        StepVerifier.create(eventSendToBridge).expectNextCount(0).verifyComplete();
    }

    @Test
    void testSendMessageEventOldNewSameState(){
        StreamsRecordProcessor srp = new StreamsRecordProcessor(availabelDocumentEventBridgeName.disponibilitaDocumentiName(), dynamoDbAsyncClient,true);

        ProcessRecordsInput processRecordsInput = new ProcessRecordsInput();
        List<Record> records = new ArrayList<>();

        com.amazonaws.services.dynamodbv2.model.Record recordDyanmo = createRecorDynamo(MODIFY_EVENT,AVAILABLE,AVAILABLE,true,AUTHORIZED_CLIENT);

        records.add(new RecordAdapter(recordDyanmo));
        processRecordsInput.withRecords(records);
        Flux<PutEventsRequestEntry> eventSendToBridge = srp.findEventSendToBridge(processRecordsInput);
        StepVerifier.create(eventSendToBridge).expectNextCount(0).verifyComplete();
    }

    @NotNull
    private  com.amazonaws.services.dynamodbv2.model.Record createRecorDynamo(String eventName ,String documentStateNew,  String documentStateOld, boolean wTags,String clientName) {
        com.amazonaws.services.dynamodbv2.model.Record recordDyanmo = new com.amazonaws.services.dynamodbv2.model.Record();
        recordDyanmo.setEventName(eventName);
        StreamRecord dynamodbRecord = new StreamRecord();
        Map<String, AttributeValue> image = new HashMap<>();

            Map<String,AttributeValue> tags = new HashMap<>();
        if(wTags){
            tags.put("tag1", new AttributeValue().withL(new AttributeValue().withS("value1"),new AttributeValue().withS("value2")));
            image.put(TAGS_KEY, createAttributeM().withM(tags));
        }
        image.put(DOCUMENTKEY_KEY, createAttributeS("111"));
        image.put(DOCUMENTSTATE_KEY, createAttributeS(documentStateNew));
        image.put(DOCUMENTTYPE_KEY, createAttributeM());
        image.get(DOCUMENTTYPE_KEY).getM().put(TIPODOCUMENTO_KEY,createAttributeS(DocTypesConstant.PN_NOTIFICATION_ATTACHMENTS));
        image.put(DOCUMENTLOGICALSTATE_KEY, createAttributeS(documentStateOld));
        image.put(CONTENTTYPE_KEY, createAttributeS(APPLICATION_PDF_VALUE));
        image.get(DOCUMENTTYPE_KEY).getM().put(CHECKSUM_KEY,createAttributeS("MD5"));
        image.put(RETENTIONUNTIL_KEY, createAttributeS("80"));
        image.put(CLIENTSHORTCODE_KEY, createAttributeS(createClient(clientName).getName()));
        dynamodbRecord.setNewImage(image);

        image = new HashMap<>();
        image.put(DOCUMENTKEY_KEY, createAttributeS("111"));
        image.put(DOCUMENTSTATE_KEY, createAttributeS(documentStateOld));
        image.put(DOCUMENTTYPE_KEY, createAttributeM());
        image.get(DOCUMENTTYPE_KEY).getM().put(TIPODOCUMENTO_KEY,createAttributeS(DocTypesConstant.PN_NOTIFICATION_ATTACHMENTS));
        image.put(DOCUMENTLOGICALSTATE_KEY, createAttributeS(SAVED));
        image.put(CONTENTTYPE_KEY, createAttributeS(APPLICATION_PDF_VALUE));
        image.get(DOCUMENTTYPE_KEY).getM().put(CHECKSUM_KEY,createAttributeS("MD5"));
        image.put(RETENTIONUNTIL_KEY, createAttributeS("80"));
        image.put(CLIENTSHORTCODE_KEY, createAttributeS(createClient(clientName).getName()));
        if (wTags)  image.put(TAGS_KEY, createAttributeM().withM(tags));

        dynamodbRecord.setOldImage(image);

        recordDyanmo.setDynamodb(dynamodbRecord);
        return recordDyanmo;

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
        AttributeValue toRet = new AttributeValue();
        Map<String, AttributeValue> map = new HashMap<>();
        toRet.setM(map);
        return toRet;

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
        AttributeValue value = new AttributeValue();
        value.setS(sVAlue);
        return value;
    }

}

