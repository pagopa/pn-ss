package it.pagopa.pnss.availableDocument;

import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.StreamRecord;
import com.amazonaws.services.dynamodbv2.streamsadapter.model.RecordAdapter;
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.RecordProcessorCheckpointer;
import com.amazonaws.services.kinesis.clientlibrary.types.ProcessRecordsInput;
import com.amazonaws.services.kinesis.model.Record;
import it.pagopa.pnss.availableDocument.event.StreamsRecordProcessor;
import it.pagopa.pnss.configurationproperties.AvailabelDocumentEventBridgeName;
import it.pagopa.pnss.testutils.annotation.SpringBootTestWebEnv;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequestEntry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static it.pagopa.pnss.availableDocument.event.ManageDynamoEvent.*;
import static it.pagopa.pnss.availableDocument.event.StreamsRecordProcessor.*;
import static it.pagopa.pnss.common.Constant.*;

@SpringBootTestWebEnv
@AutoConfigureWebTestClient
public class StreamsRecordProcessorTest {


    @Autowired
    AvailabelDocumentEventBridgeName availabelDocumentEventBridgeName;
    @Test
    public void testSendMessageEventBridgeOk(){
        StreamsRecordProcessor srp = new StreamsRecordProcessor(availabelDocumentEventBridgeName.disponibilitaDocumentiName(), true);

        ProcessRecordsInput processRecordsInput = new ProcessRecordsInput();
        List<Record> records = new ArrayList<>();

        com.amazonaws.services.dynamodbv2.model.Record recordDyanmo = createRecorDynamo(MODIFY_EVENT,AVAILABLE,SAVED);

        records.add(new RecordAdapter(recordDyanmo));
        processRecordsInput.withRecords(records);
        List<PutEventsRequestEntry> eventSendToBridge = srp.findEventSendToBridge(processRecordsInput);
        Assert.assertEquals(eventSendToBridge.size(),1);
    }

    @Test
    public void testSendMessageEventBridgeInsert(){
        StreamsRecordProcessor srp = new StreamsRecordProcessor(availabelDocumentEventBridgeName.disponibilitaDocumentiName(), true);

        ProcessRecordsInput processRecordsInput = new ProcessRecordsInput();
        List<Record> records = new ArrayList<>();

        com.amazonaws.services.dynamodbv2.model.Record recordDyanmo = createRecorDynamo(INSERT_EVENT,AVAILABLE,SAVED);

        records.add(new RecordAdapter(recordDyanmo));
        processRecordsInput.withRecords(records);
        List<PutEventsRequestEntry> eventSendToBridge = srp.findEventSendToBridge(processRecordsInput);
        Assert.assertEquals(eventSendToBridge.size(),0);
    }

    @Test
    public void testSendMessageEventBridgeDelete(){
        StreamsRecordProcessor srp = new StreamsRecordProcessor(availabelDocumentEventBridgeName.disponibilitaDocumentiName(), true);

        ProcessRecordsInput processRecordsInput = new ProcessRecordsInput();
        List<Record> records = new ArrayList<>();

        com.amazonaws.services.dynamodbv2.model.Record recordDyanmo = createRecorDynamo(REMOVE_EVENT,AVAILABLE,SAVED);

        records.add(new RecordAdapter(recordDyanmo));
        processRecordsInput.withRecords(records);
        List<PutEventsRequestEntry> eventSendToBridge = srp.findEventSendToBridge(processRecordsInput);
        Assert.assertEquals(eventSendToBridge.size(),0);
    }

    @Test
    public void testSendMessageEventOldNewSameState(){
        StreamsRecordProcessor srp = new StreamsRecordProcessor(availabelDocumentEventBridgeName.disponibilitaDocumentiName(), true);

        ProcessRecordsInput processRecordsInput = new ProcessRecordsInput();
        List<Record> records = new ArrayList<>();

        com.amazonaws.services.dynamodbv2.model.Record recordDyanmo = createRecorDynamo(MODIFY_EVENT,AVAILABLE,AVAILABLE);

        records.add(new RecordAdapter(recordDyanmo));
        processRecordsInput.withRecords(records);
        List<PutEventsRequestEntry> eventSendToBridge = srp.findEventSendToBridge(processRecordsInput);
        Assert.assertEquals(eventSendToBridge.size(),0);
    }
    @NotNull
    private  com.amazonaws.services.dynamodbv2.model.Record createRecorDynamo(String eventName ,String documentStateNew,  String documentStateOld) {
        com.amazonaws.services.dynamodbv2.model.Record recordDyanmo = new com.amazonaws.services.dynamodbv2.model.Record();
        recordDyanmo.setEventName(eventName);
        StreamRecord dynamodbRecord = new StreamRecord();
        Map<String, AttributeValue> image = new HashMap<>();
        image.put(DOCUMENTKEY_KEY, createAttributeS("111"));


        image.put(DOCUMENTSTATE_KEY, createAttributeS(documentStateNew));
        image.put(DOCUMENTTYPE_KEY, createAttributeM());
        image.get(DOCUMENTTYPE_KEY).getM().put(TIPODOCUMENTO_KEY,createAttributeS(PN_NOTIFICATION_ATTACHMENTS));
        image.put(DOCUMENTLOGICALSTATE_KEY, createAttributeS(documentStateOld));
        image.put(CONTENTTYPE_KEY, createAttributeS(APPLICATION_PDF));
        image.get(DOCUMENTTYPE_KEY).getM().put(CHECKSUM_KEY,createAttributeS("MD5"));
        image.put(RETENTIONUNTIL_KEY, createAttributeS("80"));
        image.put(CLIENTSHORTCODE_KEY, createAttributeS("pn-delivery"));
        dynamodbRecord.setNewImage(image);

        image = new HashMap<>();
        image.put(DOCUMENTKEY_KEY, createAttributeS("111"));
        image.put(DOCUMENTSTATE_KEY, createAttributeS(documentStateOld));
        image.put(DOCUMENTTYPE_KEY, createAttributeM());
        image.get(DOCUMENTTYPE_KEY).getM().put(TIPODOCUMENTO_KEY,createAttributeS(PN_NOTIFICATION_ATTACHMENTS));
        image.put(DOCUMENTLOGICALSTATE_KEY, createAttributeS(SAVED));
        image.put(CONTENTTYPE_KEY, createAttributeS(APPLICATION_PDF));
        image.get(DOCUMENTTYPE_KEY).getM().put(CHECKSUM_KEY,createAttributeS("MD5"));
        image.put(RETENTIONUNTIL_KEY, createAttributeS("80"));
        image.put(CLIENTSHORTCODE_KEY, createAttributeS("pn-delivery"));

        dynamodbRecord.setOldImage(image);

        recordDyanmo.setDynamodb(dynamodbRecord);
        return recordDyanmo;

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
