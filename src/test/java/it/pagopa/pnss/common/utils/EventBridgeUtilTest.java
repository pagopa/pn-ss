package it.pagopa.pnss.common.utils;


import it.pagopa.pnss.repositorymanager.entity.DocumentEntity;
import it.pagopa.pnss.testutils.annotation.SpringBootTestWebEnv;
import lombok.CustomLog;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequestEntry;
import  it. pagopa. pnss. repositorymanager.entity.DocTypeEntity;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

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
