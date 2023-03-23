package it.pagopa.pnss.common.exception;

import it.pagopa.pnss.common.model.pojo.DocumentStatusChange;

public class InvalidNextStatusException extends RuntimeException{

    public InvalidNextStatusException(DocumentStatusChange documentStatusChange) {
        super(String.format("Status change from %s to %s is not valid for client %s within the process with id %s",
                documentStatusChange.getCurrentStatus(),
                documentStatusChange.getNextStatus(),
                documentStatusChange.getXPagopaExtchCxId(),
                documentStatusChange.getProcessId()));
    }

    public InvalidNextStatusException(String nextStatus, String fileKey) {
        super(String.format("The status %s is not valid for the file key %s", nextStatus, fileKey));
    }
}
