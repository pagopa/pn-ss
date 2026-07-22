package it.pagopa.pnss.common.utils;


import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pnss.availabledocument.dto.NotificationMessage;
import it.pagopa.pnss.repositorymanager.entity.DocTypeEntity;
import it.pagopa.pnss.repositorymanager.entity.DocumentEntity;
import it.pagopa.pnss.testutils.annotation.SpringBootTestWebEnv;
import lombok.CustomLog;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequestEntry;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringBootTestWebEnv
@CustomLog
class EventBridgeUtilTest {
    @Test
    void createMessageWithValidDocumentEntity() {
        DocumentEntity documentEntity = mock(DocumentEntity.class);
        when(documentEntity.getDocumentKey()).thenReturn("key");
        when(documentEntity.getDocumentType()).thenReturn(new DocTypeEntity());
        when(documentEntity.getDocumentLogicalState()).thenReturn("state");
        when(documentEntity.getContentType()).thenReturn("contentType");
        when(documentEntity.getCheckSum()).thenReturn("checksum");
        when(documentEntity.getRetentionUntil()).thenReturn("");
        when(documentEntity.getClientShortCode()).thenReturn("shortCode");
        when(documentEntity.getTags()).thenReturn(Map.of("tag1", List.of("value1")));

        String disponibilitaDocumentiEventBridge = "eventBridge";
        String oldDocumentState = "state";
        Boolean canReadTags = true;

        PutEventsRequestEntry result = EventBridgeUtil.createMessage(documentEntity, disponibilitaDocumentiEventBridge, oldDocumentState, canReadTags);

        assertNotNull(result);
        assertEquals("eventBridge", result.eventBusName());
    }

    @Test
    void createMessageWithNullDocumentType() {
        DocumentEntity documentEntity = mock(DocumentEntity.class);
        when(documentEntity.getDocumentKey()).thenReturn("key");
        when(documentEntity.getDocumentType()).thenReturn(null);
        when(documentEntity.getDocumentLogicalState()).thenReturn("state");
        when(documentEntity.getContentType()).thenReturn("contentType");
        when(documentEntity.getCheckSum()).thenReturn("checksum");
        when(documentEntity.getRetentionUntil()).thenReturn("");
        when(documentEntity.getClientShortCode()).thenReturn("shortCode");
        when(documentEntity.getTags()).thenReturn(Map.of("tag1", List.of("value1")));

        String disponibilitaDocumentiEventBridge = "eventBridge";
        String oldDocumentState = "state";
        Boolean canReadTags = true;

        PutEventsRequestEntry result = EventBridgeUtil.createMessage(documentEntity, disponibilitaDocumentiEventBridge, oldDocumentState, canReadTags);

        assertNotNull(result);
        assertEquals("eventBridge", result.eventBusName());
    }

    @Test
    void createMessageRemovesLocalTagPrefixFromEvent() throws Exception {
        DocumentEntity documentEntity = mock(DocumentEntity.class);
        when(documentEntity.getDocumentKey()).thenReturn("key");
        when(documentEntity.getDocumentType()).thenReturn(new DocTypeEntity());
        when(documentEntity.getDocumentLogicalState()).thenReturn("state");
        when(documentEntity.getContentType()).thenReturn("contentType");
        when(documentEntity.getCheckSum()).thenReturn("checksum");
        when(documentEntity.getRetentionUntil()).thenReturn("");
        when(documentEntity.getClientShortCode()).thenReturn("shortCode");
        when(documentEntity.getTags()).thenReturn(Map.of(
                "pn-radd-fsu~DataCreazione", List.of("2024-01-01"),
                "IUN", List.of("TEST-IUN")));

        PutEventsRequestEntry result = EventBridgeUtil.createMessage(documentEntity, "eventBridge", "state", true);

        NotificationMessage message = new ObjectMapper().readValue(result.detail(), NotificationMessage.class);
        assertNotNull(message.getTags());
        assertTrue(message.getTags().containsKey("DataCreazione"));
        assertFalse(message.getTags().containsKey("pn-radd-fsu~DataCreazione"));
        assertTrue(message.getTags().containsKey("IUN"));
    }

    @Test
    void createMessageWithNullTags() {
        DocumentEntity documentEntity = mock(DocumentEntity.class);
        when(documentEntity.getDocumentKey()).thenReturn("key");
        when(documentEntity.getDocumentType()).thenReturn(new DocTypeEntity());
        when(documentEntity.getDocumentLogicalState()).thenReturn("state");
        when(documentEntity.getContentType()).thenReturn("contentType");
        when(documentEntity.getCheckSum()).thenReturn("checksum");
        when(documentEntity.getRetentionUntil()).thenReturn("");
        when(documentEntity.getClientShortCode()).thenReturn("shortCode");
        when(documentEntity.getTags()).thenReturn(null);

        String disponibilitaDocumentiEventBridge = "eventBridge";
        String oldDocumentState = "state";
        Boolean canReadTags = true;

        PutEventsRequestEntry result = EventBridgeUtil.createMessage(documentEntity, disponibilitaDocumentiEventBridge, oldDocumentState, canReadTags);

        assertNotNull(result);
        assertEquals("eventBridge", result.eventBusName());
    }
}
