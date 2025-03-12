package it.pagopa.pnss.transformation.exception;


/**
 * Signals that a given status associated with a document is not valid for the transformation.
 */
public class InvalidDocumentStateException extends RuntimeException {

    public InvalidDocumentStateException(String documentState, String fileKey) {
        super(String.format("Status '%s' is not valid for transformation for document '%s'", documentState, fileKey));
    }

}
