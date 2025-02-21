
package it.pagopa.pnss.common.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pnss.availabledocument.dto.NotificationMessage;
import it.pagopa.pnss.common.exception.PutEventsRequestEntryException;
import it.pagopa.pnss.repositorymanager.entity.DocumentEntity;
import lombok.CustomLog;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequestEntry;

import java.util.*;

import static it.pagopa.pnss.common.constant.Constant.*;

@CustomLog
public class EventBridgeUtil {


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
    public static final String UNAVAILABILITY_DOCUMENT_STATUS = "ERROR";
    private static final ObjectMapper objMap = new ObjectMapper();

    public static PutEventsRequestEntry createMessage(DocumentEntity documentEntity, String disponibilitaDocumentiEventBridge, String oldDocumentState, Boolean canReadTags){
        String key = documentEntity.getDocumentKey();
        NotificationMessage message = new NotificationMessage();

        message.setKey(key);
        message.setVersionId("01");

        message.setDocumentType(documentEntity.getDocumentType()!=null && documentEntity.getDocumentType().getTipoDocumento()!= null ?
                documentEntity.getDocumentType().getTipoDocumento():null);

        message.setDocumentStatus(documentEntity.getDocumentLogicalState()!=null ? documentEntity.getDocumentLogicalState().toString():null);
        message.setContentType(documentEntity.getContentType()!=null ? documentEntity.getContentType().toString():null);

        message.setChecksum(documentEntity.getCheckSum()!=null ? documentEntity.getCheckSum().toString():null);

        message.setRetentionUntil(documentEntity.getRetentionUntil()!=null ? documentEntity.getRetentionUntil().toString():null);
        message.setClientShortCode(documentEntity.getClientShortCode()!=null ? documentEntity.getClientShortCode().toString():null);

        if (documentEntity.getTags() != null && canReadTags) {
            Map<String, List<String>> tags =documentEntity.getTags();
            message.setTags(tags);
        }

        try {
            String event = objMap.writeValueAsString(message);
            return createPutEventRequestEntry(event, disponibilitaDocumentiEventBridge, oldDocumentState.equalsIgnoreCase(FREEZED) ? EVENT_BUS_SOURCE_GLACIER_DOCUMENTS : EVENT_BUS_SOURCE_AVAILABLE_DOCUMENT);
        } catch (JsonProcessingException e) {
            throw new PutEventsRequestEntryException(PutEventsRequestEntry.class);
        }
    }

    public static PutEventsRequestEntry createUnavailabilityMessage(DocumentEntity documentEntity, String fileKey, String eventBusName){
        NotificationMessage message = new NotificationMessage();

        message.setKey(fileKey);
        message.setDocumentStatus(UNAVAILABILITY_DOCUMENT_STATUS);
        message.setContentType(documentEntity.getContentType()!=null ? documentEntity.getContentType():null);
        message.setChecksum(documentEntity.getCheckSum()!=null ? documentEntity.getCheckSum():null);
        message.setClientShortCode(documentEntity.getClientShortCode()!=null ? documentEntity.getClientShortCode():null);

        try {
            String event = objMap.writeValueAsString(message);
            return createPutEventRequestEntry(event, eventBusName, EVENT_BUS_SOURCE_TRANSFORMATION_DOCUMENT);
        } catch (JsonProcessingException e) {
            throw new PutEventsRequestEntryException(PutEventsRequestEntry.class);
        }
    }

    private static PutEventsRequestEntry createPutEventRequestEntry(String event, String eventBusName, String detailType){
        return  PutEventsRequestEntry.builder()
                .time(new Date().toInstant())
                .source(GESTORE_DISPONIBILITA_EVENT_NAME)
                .detailType(detailType)
                .eventBusName(eventBusName)
                .detail(event).build();

    }

}

