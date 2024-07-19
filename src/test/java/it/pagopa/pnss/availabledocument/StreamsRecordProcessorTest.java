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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClientBuilder;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequestEntry;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static it.pagopa.pnss.availabledocument.event.ManageDynamoEvent.*;
import static it.pagopa.pnss.availabledocument.event.StreamsRecordProcessor.*;
import static it.pagopa.pnss.common.constant.Constant.*;
import static org.springframework.http.MediaType.APPLICATION_PDF_VALUE;

@SpringBootTestWebEnv
@AutoConfigureWebTestClient
class StreamsRecordProcessorTest {


    DynamoDbClientBuilder dynamoDbClientBuilder;
    DynamoDbClient dynamoDbClient;

    @Autowired
    AvailabelDocumentEventBridgeName availabelDocumentEventBridgeName;

    @BeforeEach
    void setUp() {
        dynamoDbClientBuilder = DynamoDbClient.builder().region(Region.EU_CENTRAL_1);
        dynamoDbClient = dynamoDbClientBuilder.build();

        putAnagraficaClient(createClient());
    }

    @Test
    void testProcessRecordsWithoutPermissions()  {
        StreamsRecordProcessor srp = new StreamsRecordProcessor(availabelDocumentEventBridgeName.disponibilitaDocumentiName(), true);

        UserConfiguration client = createClient();
        client.setCanReadTags(false);
        putAnagraficaClient(client);

        ProcessRecordsInput processRecordsInput = new ProcessRecordsInput();
        List<Record> records = new ArrayList<>();

        com.amazonaws.services.dynamodbv2.model.Record recordDyanmo = createRecorDynamo(MODIFY_EVENT,AVAILABLE,BOOKED);

        records.add(new RecordAdapter(recordDyanmo));
        processRecordsInput.withRecords(records);
        Flux<PutEventsRequestEntry> eventSendToBridge = srp.findEventSendToBridge(processRecordsInput);
        StepVerifier.create(eventSendToBridge).assertNext(putEventsRequestEntry -> {
            NotificationMessage notificationMessage = eventToNotificationMessage(putEventsRequestEntry.detail());
            Assertions.assertNull(notificationMessage.getTags());
        }).verifyComplete();
    }

    @Test
    void testProcessRecordsWithPermissions(){
        StreamsRecordProcessor srp = new StreamsRecordProcessor(availabelDocumentEventBridgeName.disponibilitaDocumentiName(), true);

        ProcessRecordsInput processRecordsInput = new ProcessRecordsInput();
        List<Record> records = new ArrayList<>();

        com.amazonaws.services.dynamodbv2.model.Record recordDyanmo = createRecorDynamo(MODIFY_EVENT,AVAILABLE,BOOKED);

        records.add(new RecordAdapter(recordDyanmo));
        processRecordsInput.withRecords(records);
        Flux<PutEventsRequestEntry> eventSendToBridge = srp.findEventSendToBridge(processRecordsInput);
        StepVerifier.create(eventSendToBridge).assertNext(putEventsRequestEntry -> {
            NotificationMessage notificationMessage = eventToNotificationMessage(putEventsRequestEntry.detail());
            Assertions.assertNotNull(notificationMessage.getTags());
        }).verifyComplete();
    }
    @Test
    void testProcessRecordsOk() {
        StreamsRecordProcessor srp = new StreamsRecordProcessor(availabelDocumentEventBridgeName.disponibilitaDocumentiName(), true);

        ProcessRecordsInput processRecordsInput = new ProcessRecordsInput();
        List<Record> records = new ArrayList<>();

        com.amazonaws.services.dynamodbv2.model.Record recordDyanmo = createRecorDynamo(MODIFY_EVENT, AVAILABLE, BOOKED);

        records.add(new RecordAdapter(recordDyanmo));
        processRecordsInput.withRecords(records);

        Assertions.assertDoesNotThrow(() -> srp.processRecords(processRecordsInput));
    }

    @Test
    void testSendMessageEventBridgeOk() {
        StreamsRecordProcessor srp = new StreamsRecordProcessor(availabelDocumentEventBridgeName.disponibilitaDocumentiName(), true);

        ProcessRecordsInput processRecordsInput = new ProcessRecordsInput();
        List<Record> records = new ArrayList<>();

        com.amazonaws.services.dynamodbv2.model.Record recordDyanmo = createRecorDynamo(MODIFY_EVENT,AVAILABLE,BOOKED);

        records.add(new RecordAdapter(recordDyanmo));
        processRecordsInput.withRecords(records);
        Flux<PutEventsRequestEntry> eventSendToBridge = srp.findEventSendToBridge(processRecordsInput);
        StepVerifier.create(eventSendToBridge).expectNextCount(1).verifyComplete();
    }

    @Test
    void testSendMessageEventBridgeInsert() {
        StreamsRecordProcessor srp = new StreamsRecordProcessor(availabelDocumentEventBridgeName.disponibilitaDocumentiName(), true);

        ProcessRecordsInput processRecordsInput = new ProcessRecordsInput();
        List<Record> records = new ArrayList<>();

        com.amazonaws.services.dynamodbv2.model.Record recordDyanmo = createRecorDynamo(INSERT_EVENT,AVAILABLE,BOOKED);

        records.add(new RecordAdapter(recordDyanmo));
        processRecordsInput.withRecords(records);
        Flux<PutEventsRequestEntry> eventSendToBridge = srp.findEventSendToBridge(processRecordsInput);
        StepVerifier.create(eventSendToBridge).expectNextCount(0).verifyComplete();
    }

    @Test
    void testSendMessageEventBridgeDelete(){
        StreamsRecordProcessor srp = new StreamsRecordProcessor(availabelDocumentEventBridgeName.disponibilitaDocumentiName(), true);

        ProcessRecordsInput processRecordsInput = new ProcessRecordsInput();
        List<Record> records = new ArrayList<>();

        com.amazonaws.services.dynamodbv2.model.Record recordDyanmo = createRecorDynamo(REMOVE_EVENT,AVAILABLE,BOOKED);

        records.add(new RecordAdapter(recordDyanmo));
        processRecordsInput.withRecords(records);
        Flux<PutEventsRequestEntry> eventSendToBridge = srp.findEventSendToBridge(processRecordsInput);
        StepVerifier.create(eventSendToBridge).expectNextCount(0).verifyComplete();
    }

    @Test
    void testSendMessageEventOldNewSameState(){
        StreamsRecordProcessor srp = new StreamsRecordProcessor(availabelDocumentEventBridgeName.disponibilitaDocumentiName(), true);

        ProcessRecordsInput processRecordsInput = new ProcessRecordsInput();
        List<Record> records = new ArrayList<>();

        com.amazonaws.services.dynamodbv2.model.Record recordDyanmo = createRecorDynamo(MODIFY_EVENT,AVAILABLE,AVAILABLE);

        records.add(new RecordAdapter(recordDyanmo));
        processRecordsInput.withRecords(records);
        Flux<PutEventsRequestEntry> eventSendToBridge = srp.findEventSendToBridge(processRecordsInput);
        StepVerifier.create(eventSendToBridge).expectNextCount(0).verifyComplete();
    }

    @NotNull
    private  com.amazonaws.services.dynamodbv2.model.Record createRecorDynamo(String eventName ,String documentStateNew,  String documentStateOld) {
        com.amazonaws.services.dynamodbv2.model.Record recordDyanmo = new com.amazonaws.services.dynamodbv2.model.Record();
        recordDyanmo.setEventName(eventName);
        StreamRecord dynamodbRecord = new StreamRecord();
        Map<String, AttributeValue> image = new HashMap<>();
        Map<String,AttributeValue> tags = new HashMap<>();
        tags.put("tag1", new AttributeValue().withL(new AttributeValue().withS("value1"),new AttributeValue().withS("value2")));
        image.put(DOCUMENTKEY_KEY, createAttributeS("111"));


        image.put(DOCUMENTSTATE_KEY, createAttributeS(documentStateNew));
        image.put(DOCUMENTTYPE_KEY, createAttributeM());
        image.get(DOCUMENTTYPE_KEY).getM().put(TIPODOCUMENTO_KEY,createAttributeS(DocTypesConstant.PN_NOTIFICATION_ATTACHMENTS));
        image.put(DOCUMENTLOGICALSTATE_KEY, createAttributeS(documentStateOld));
        image.put(CONTENTTYPE_KEY, createAttributeS(APPLICATION_PDF_VALUE));
        image.get(DOCUMENTTYPE_KEY).getM().put(CHECKSUM_KEY,createAttributeS("MD5"));
        image.put(RETENTIONUNTIL_KEY, createAttributeS("80"));
        image.put(CLIENTSHORTCODE_KEY, createAttributeS("pn-delivery"));
        image.put(TAGS_KEY, createAttributeM().withM(tags));
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
        image.put(CLIENTSHORTCODE_KEY, createAttributeS("pn-delivery"));
        image.put(TAGS_KEY, createAttributeM().withM(tags));

        dynamodbRecord.setOldImage(image);

        recordDyanmo.setDynamodb(dynamodbRecord);
        return recordDyanmo;

    }

    private void putAnagraficaClient(UserConfiguration client) {
        dynamoDbClient.putItem(request -> request.tableName("pn-SsAnagraficaClient")
                .item(Map.of("name", software.amazon.awssdk.services.dynamodb.model.AttributeValue.builder().s(client.getName()).build(),
                        "canReadTags", software.amazon.awssdk.services.dynamodb.model.AttributeValue.builder().bool(client.getCanReadTags()).build(),
                        "canWriteTags", software.amazon.awssdk.services.dynamodb.model.AttributeValue.builder().bool(client.getCanWriteTags()).build(),
                        "canExecutePatch", software.amazon.awssdk.services.dynamodb.model.AttributeValue.builder().bool(client.getCanExecutePatch()).build(),
                        "apiKey", software.amazon.awssdk.services.dynamodb.model.AttributeValue.builder().s(client.getApiKey()).build())));

    }
    private static UserConfiguration createClient() {
        UserConfiguration client = new UserConfiguration();
        client.setName("pn-delivery");
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



    @NotNull
    private static AttributeValue createAttributeS(String sVAlue) {
        AttributeValue value = new AttributeValue();
        value.setS(sVAlue);
        return value;
    }

}

