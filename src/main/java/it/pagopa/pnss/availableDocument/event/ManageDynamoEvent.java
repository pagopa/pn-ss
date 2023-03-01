package it.pagopa.pnss.availableDocument.event;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pnss.availableDocument.dto.NotificationMessage;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequestEntry;

import java.util.Date;
import java.util.Map;

import static it.pagopa.pnss.common.Constant.AVAILABLE;
import static it.pagopa.pnss.common.Constant.EVEN_BUS_SOURCE_AVAILABLE_DOCUMENT;

public class ManageDynamoEvent {



    public PutEventsRequestEntry manageItem(AmazonDynamoDB dynamoDBClient, String tableName,
                                            String disponibilitaDocumentiEventBridge, Map<String, AttributeValue> newImage,
                                            Map<String, AttributeValue> oldImage) {
        String oldDocumentState = oldImage.get("documentState").getS();
        String newDocumentState = newImage.get("documentState").getS();

        if (!oldDocumentState.equalsIgnoreCase(newDocumentState) && newDocumentState.equalsIgnoreCase(AVAILABLE)){
            return  createMessage(newImage, disponibilitaDocumentiEventBridge);

        }


        return null;
    }

    public PutEventsRequestEntry createMessage(Map<String, AttributeValue> docEntity, String disponibilitaDocumentiEventBridge){

        NotificationMessage message = new NotificationMessage();

        message.setKey(docEntity.get("documentKey").getS());
        message.setVersionId("01");

        message.setDocumentType(docEntity.get("documentType").getM()!=null && docEntity.get("documentType").getM().get("tipoDocumento")!=null ?
                docEntity.get("documentType").getM().get("tipoDocumento").getS():null);

        message.setDocumentStatus(docEntity.get("documentLogicalState")!=null ? docEntity.get("documentLogicalState").getS():null);
        message.setContentType(docEntity.get("contentType")!=null ? docEntity.get("contentType").getS():null);

        message.setChecksum(docEntity.get("documentType").getM()!=null && docEntity.get("documentType").getM().get("checksum")!=null ?
                docEntity.get("documentType").getM().get("checksum").getS():null);

        message.setRetentionUntil(docEntity.get("retentionUntil")!=null ? docEntity.get("retentionUntil").getS(): null);
        message.setClientShortCode(docEntity.get("clientShortCode")!=null ? docEntity.get("clientShortCode").getS(): null);
        ObjectMapper objMap = new ObjectMapper();

        try {
            String event = objMap.writeValueAsString(message);
            return creatPutEventRequestEntry(event,docEntity.get("documentKey").getS(), disponibilitaDocumentiEventBridge );
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private PutEventsRequestEntry creatPutEventRequestEntry(String event, String detailType, String disponibilitaDocumentiEventBridge) {
        return  PutEventsRequestEntry.builder()
                .time(new Date().toInstant())
                .source("GESTORE DISPONIBILITA")
                .detailType(EVEN_BUS_SOURCE_AVAILABLE_DOCUMENT)
                .eventBusName(disponibilitaDocumentiEventBridge)

                .detail(event).build();

    }
}
