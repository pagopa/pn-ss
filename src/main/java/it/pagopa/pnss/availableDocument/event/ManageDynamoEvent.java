package it.pagopa.pnss.availableDocument.event;

import it.pagopa.pnss.availableDocument.dto.NotificationMessage;
import it.pagopa.pnss.repositorymanager.entity.DocumentEntity;

import static it.pagopa.pnss.common.Constant.AVAILABLE;

public class ManageDynamoEvent {

    public void execute (DocumentEntity docEntity){

        NotificationMessage message = new NotificationMessage();
        if (docEntity.getDocumentState().equalsIgnoreCase(AVAILABLE)){
            message.setKey(docEntity.getDocumentKey());
            //todo chiedere cosa mettere come version ID
            message.setVersionId("versionId");
            message.setDocumentType(docEntity.getDocumentType().getTipoDocumento());
            message.setDocumentStatus(docEntity.getDocumentLogicalState());
            message.setContentType(docEntity.getContentType());
            message.setChecksum(docEntity.getDocumentType().getChecksum());
            message.setRetentionUntil(docEntity.getRetentionUntil());
            //todo mettere  ID del cliente

            message.setClientShortCode(docEntity.getDocumentType().getInitialStatus());
        }
    }
}
