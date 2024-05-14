package it.pagopa.pnss.repositorymanager.exception;

import java.io.Serializable;

public class InvalidRetentionException extends RepositoryManagerException implements Serializable {

        private static final long serialVersionUID = 3917678876682098794L;

        public InvalidRetentionException() {
            super("Invalid retention date");
        }

        public InvalidRetentionException(String msg) {
            super(msg);
        }
}
