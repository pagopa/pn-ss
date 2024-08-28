package it.pagopa.pnss.availabledocument;

import com.amazonaws.SdkClientException;
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
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;
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

import static it.pagopa.pnss.availabledocument.event.ManageDynamoEvent.*;
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

    @BeforeEach
    void setUp() {
        System.setProperty("pn.ss.safe-clients","pn-delivery;pn-test");
        putAnagraficaClient(createClient(true));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(booleans = {false})
    void testProcessRecordsWithoutPermissions(Boolean canReadTagsValue)  {
        StreamsRecordProcessor srp = new StreamsRecordProcessor(availabelDocumentEventBridgeName.disponibilitaDocumentiName(), dynamoDbAsyncClient,true);

        UserConfiguration client = createClient(true);
        client.setCanReadTags(canReadTagsValue);
        putAnagraficaClient(client);

        ProcessRecordsInput processRecordsInput = new ProcessRecordsInput();
        List<Record> records = new ArrayList<>();

        com.amazonaws.services.dynamodbv2.model.Record recordDyanmo = createRecorDynamo(MODIFY_EVENT,AVAILABLE,BOOKED,true,true);

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

        com.amazonaws.services.dynamodbv2.model.Record recordDyanmo = createRecorDynamo(MODIFY_EVENT,AVAILABLE,BOOKED,true,true);

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

        com.amazonaws.services.dynamodbv2.model.Record recordDyanmo = createRecorDynamo(MODIFY_EVENT, AVAILABLE, BOOKED,true,true);

        records.add(new RecordAdapter(recordDyanmo));
        processRecordsInput.withRecords(records);

        Assertions.assertDoesNotThrow(() -> srp.processRecords(processRecordsInput));
    }
    @ParameterizedTest
    @MethodSource("provideClientsAndTagsParameters")
    void testProcessRecordsWithDifferentConditions(boolean clientAuthorized, boolean withTags, int expectedCount) {
        StreamsRecordProcessor srp = new StreamsRecordProcessor(availabelDocumentEventBridgeName.disponibilitaDocumentiName(), dynamoDbAsyncClient,true);

        UserConfiguration client = createClient(clientAuthorized);
        putAnagraficaClient(client);

        ProcessRecordsInput processRecordsInput = new ProcessRecordsInput();
        List<Record> records = new ArrayList<>();

        com.amazonaws.services.dynamodbv2.model.Record recordDyanmo = createRecorDynamo(MODIFY_EVENT,AVAILABLE,BOOKED,withTags,clientAuthorized);


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


        com.amazonaws.services.dynamodbv2.model.Record recordDyanmo = createRecorDynamo(MODIFY_EVENT,AVAILABLE,BOOKED,true,true);
        StreamsRecordProcessor srpSpy = spy(srp);
        doThrow(SdkClientException.class).when(srpSpy).getFromDynamo(anyString());
        doCallRealMethod().when(srpSpy).getCanReadTags(anyString());
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

        com.amazonaws.services.dynamodbv2.model.Record recordDyanmo = createRecorDynamo(MODIFY_EVENT,AVAILABLE,BOOKED,true,true);

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

        com.amazonaws.services.dynamodbv2.model.Record recordDyanmo = createRecorDynamo(INSERT_EVENT,AVAILABLE,BOOKED,true,true);

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

        com.amazonaws.services.dynamodbv2.model.Record recordDyanmo = createRecorDynamo(REMOVE_EVENT,AVAILABLE,BOOKED,true,true);

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

        com.amazonaws.services.dynamodbv2.model.Record recordDyanmo = createRecorDynamo(MODIFY_EVENT,AVAILABLE,AVAILABLE,true,true);

        records.add(new RecordAdapter(recordDyanmo));
        processRecordsInput.withRecords(records);
        Flux<PutEventsRequestEntry> eventSendToBridge = srp.findEventSendToBridge(processRecordsInput);
        StepVerifier.create(eventSendToBridge).expectNextCount(0).verifyComplete();
    }

    @NotNull
    private  com.amazonaws.services.dynamodbv2.model.Record createRecorDynamo(String eventName ,String documentStateNew,  String documentStateOld, boolean wTags,boolean clientAuthorized) {
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
        image.put(CLIENTSHORTCODE_KEY, createAttributeS(createClient(clientAuthorized).getName()));
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
        image.put(CLIENTSHORTCODE_KEY, createAttributeS(createClient(clientAuthorized).getName()));
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

    private static UserConfiguration createClient(boolean authorized) {
        UserConfiguration client = new UserConfiguration();
        String clientName = authorized ? "pn-delivery" : "pn-delivery-unauthorized";
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
                Arguments.of(false, true, 1),  // testProcessRecordsWithClientNotInList
                Arguments.of(true, false, 1),  // testProcessRecordsWithoutTags
                Arguments.of(false, false, 0)  // testProcessRecordsWithoutTagsWithClientNotInList
        );
    }

    @NotNull
    private static AttributeValue createAttributeS(String sVAlue) {
        AttributeValue value = new AttributeValue();
        value.setS(sVAlue);
        return value;
    }

}

