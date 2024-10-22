package it.pagopa.pnss.repositorymanager.exception;

public class ResourceDeletedException extends RuntimeException {
    public static class DocumentDeletedException extends RuntimeException {

        public DocumentDeletedException(String fileKey) {
            super(String.format("The document with key '%s' has been deleted.", fileKey));
        }
    }

}
