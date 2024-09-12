
package it.pagopa.pnss.availabledocument.event;

import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pnss.availabledocument.dto.NotificationMessage;
import it.pagopa.pnss.common.exception.PutEventsRequestEntryException;
import lombok.CustomLog;
import org.slf4j.MDC;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequestEntry;

import java.util.*;

import static it.pagopa.pnss.common.constant.Constant.*;
import static it.pagopa.pnss.common.utils.LogUtils.MDC_CORR_ID_KEY;

@CustomLog
public class ManageDynamoEvent {


    public static final String DOCUMENTSTATE_KEY = "documentState";
    public static final String DOCUMENTKEY_KEY = "documentKey";
    public static final String DOCUMENTTYPE_KEY = "documentType";
    public static final String TIPODOCUMENTO_KEY = "tipoDocumento";
    public static final String DOCUMENTLOGICALSTATE_KEY = "documentLogicalState";
    public static final String CHECKSUM_KEY = "checkSum";
    public static final String RETENTIONUNTIL_KEY = "retentionUntil";
    public static final String CONTENTTYPE_KEY = "contentType";
    public static final String CLIENTSHORTCODE_KEY = "clientShortCode";
    public static final String TAGS_KEY = "tags";

    public PutEventsRequestEntry createMessage(Map<String, AttributeValue> docEntity, String disponibilitaDocumentiEventBridge, String oldDocumentState, Boolean canReadTags){
        String key = docEntity.get(DOCUMENTKEY_KEY).getS();
        MDC.put(MDC_CORR_ID_KEY, key);
        NotificationMessage message = new NotificationMessage();

        message.setKey(key);
        message.setVersionId("01");

        message.setDocumentType(docEntity.get(DOCUMENTTYPE_KEY).getM()!=null && docEntity.get(DOCUMENTTYPE_KEY).getM().get(TIPODOCUMENTO_KEY)!=null ?
                docEntity.get(DOCUMENTTYPE_KEY).getM().get(TIPODOCUMENTO_KEY).getS():null);

        message.setDocumentStatus(docEntity.get(DOCUMENTLOGICALSTATE_KEY)!=null ? docEntity.get(DOCUMENTLOGICALSTATE_KEY).getS():null);
        message.setContentType(docEntity.get(CONTENTTYPE_KEY)!=null ? docEntity.get(CONTENTTYPE_KEY).getS():null);

        message.setChecksum(docEntity.get(CHECKSUM_KEY)!=null ? docEntity.get(CHECKSUM_KEY).getS(): null);

        message.setRetentionUntil(docEntity.get(RETENTIONUNTIL_KEY)!=null ? docEntity.get(RETENTIONUNTIL_KEY).getS(): null);
        message.setClientShortCode(docEntity.get(CLIENTSHORTCODE_KEY)!=null ? docEntity.get(CLIENTSHORTCODE_KEY).getS(): null);

        if (docEntity.get(TAGS_KEY) != null && canReadTags) {
            Map<String, AttributeValue> tagsMap = docEntity.get(TAGS_KEY).getM();
            Map<String, List<String>> tags = new HashMap<>();
            for (Map.Entry<String, AttributeValue> entry : tagsMap.entrySet()) {
                List<String> tagValues = new ArrayList<>();
                for (AttributeValue value : entry.getValue().getL()) {
                    tagValues.add(value.getS());
                }
                tags.put(entry.getKey(), tagValues);
            }
            message.setTags(tags);
        }

        ObjectMapper objMap = new ObjectMapper();

        try {
            String event = objMap.writeValueAsString(message);
            return createPutEventRequestEntry(event, disponibilitaDocumentiEventBridge,oldDocumentState);
        } catch (JsonProcessingException e) {
            throw new PutEventsRequestEntryException(PutEventsRequestEntry.class);
        }
    }

    private PutEventsRequestEntry createPutEventRequestEntry(String event, String disponibilitaDocumentiEventBridge, String oldDocumentState){
        return  PutEventsRequestEntry.builder()
                .time(new Date().toInstant())
                .source(GESTORE_DISPONIBILITA_EVENT_NAME)
                .detailType(oldDocumentState.equalsIgnoreCase(FREEZED) ? EVENT_BUS_SOURCE_GLACIER_DOCUMENTS : EVENT_BUS_SOURCE_AVAILABLE_DOCUMENT)
                .eventBusName(disponibilitaDocumentiEventBridge)

                .detail(event).build();

    }
}

