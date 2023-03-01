package it.pagopa.pnss.availableDocument.event;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pnss.availableDocument.dto.NotificationMessage;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequestEntry;

import java.util.Date;
import java.util.Map;

import static it.pagopa.pnss.common.Constant.*;

public class ManageDynamoEvent {


    public static final String DOCUMENTSTATE_KEY = "documentState";
    public static final String DOCUMENTKEY_KEY = "documentKey";
    public static final String DOCUMENTTYPE_KEY = "documentType";
    public static final String TIPODOCUMENTO_KEY = "tipoDocumento";
    public static final String DOCUMENTLOGICALSTATE_KEY = "documentLogicalState";
    public static final String CHECKSUM_KEY = "checksum";
    public static final String RETENTIONUNTIL_KEY = "retentionUntil";
    public static final String CONTENTTYPE_KEY = "contentType";
    public static final String CLIENTSHORTCODE_KEY = "clientShortCode";

    public PutEventsRequestEntry manageItem(String disponibilitaDocumentiEventBridge, Map<String, AttributeValue> newImage,
                                            Map<String, AttributeValue> oldImage) {
        String oldDocumentState = oldImage.get(DOCUMENTSTATE_KEY).getS();
        String newDocumentState = newImage.get(DOCUMENTSTATE_KEY).getS();

        if (!oldDocumentState.equalsIgnoreCase(newDocumentState) && newDocumentState.equalsIgnoreCase(AVAILABLE)){
            return  createMessage(newImage, disponibilitaDocumentiEventBridge);

        }


        return null;
    }

    public PutEventsRequestEntry createMessage(Map<String, AttributeValue> docEntity, String disponibilitaDocumentiEventBridge){

        NotificationMessage message = new NotificationMessage();

        message.setKey(docEntity.get(DOCUMENTKEY_KEY).getS());
        message.setVersionId("01");

        message.setDocumentType(docEntity.get(DOCUMENTTYPE_KEY).getM()!=null && docEntity.get(DOCUMENTTYPE_KEY).getM().get(TIPODOCUMENTO_KEY)!=null ?
                docEntity.get(DOCUMENTTYPE_KEY).getM().get(TIPODOCUMENTO_KEY).getS():null);

        message.setDocumentStatus(docEntity.get(DOCUMENTLOGICALSTATE_KEY)!=null ? docEntity.get(DOCUMENTLOGICALSTATE_KEY).getS():null);
        message.setContentType(docEntity.get(CONTENTTYPE_KEY)!=null ? docEntity.get(CONTENTTYPE_KEY).getS():null);

        message.setChecksum(docEntity.get(DOCUMENTTYPE_KEY).getM()!=null && docEntity.get(DOCUMENTTYPE_KEY).getM().get(CHECKSUM_KEY)!=null ?
                docEntity.get(DOCUMENTTYPE_KEY).getM().get(CHECKSUM_KEY).getS():null);

        message.setRetentionUntil(docEntity.get(RETENTIONUNTIL_KEY)!=null ? docEntity.get(RETENTIONUNTIL_KEY).getS(): null);
        message.setClientShortCode(docEntity.get(CLIENTSHORTCODE_KEY)!=null ? docEntity.get(CLIENTSHORTCODE_KEY).getS(): null);
        ObjectMapper objMap = new ObjectMapper();

        try {
            String event = objMap.writeValueAsString(message);
            return creatPutEventRequestEntry(event,docEntity.get(DOCUMENTKEY_KEY).getS(), disponibilitaDocumentiEventBridge );
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private PutEventsRequestEntry creatPutEventRequestEntry(String event, String detailType, String disponibilitaDocumentiEventBridge) {
        return  PutEventsRequestEntry.builder()
                .time(new Date().toInstant())
                .source(GESTORE_DISPONIBILITA_EVENT_NAME)
                .detailType(EVEN_BUS_SOURCE_AVAILABLE_DOCUMENT)
                .eventBusName(disponibilitaDocumentiEventBridge)

                .detail(event).build();

    }
}
