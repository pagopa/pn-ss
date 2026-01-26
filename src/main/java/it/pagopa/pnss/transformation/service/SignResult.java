package it.pagopa.pnss.transformation.service;

import it.pagopa.pn.library.sign.pojo.PnSignDocumentResponse;

public interface SignResult {

    final class Success implements SignResult {
        private final PnSignDocumentResponse response;

        public Success(PnSignDocumentResponse response) {
            this.response = response;
        }

        public PnSignDocumentResponse response() {
            return response;
        }
    }

    final class TemporaryError implements SignResult {
        private final Throwable cause;

        public TemporaryError(Throwable cause) {
            this.cause = cause;
        }

        public Throwable cause() {
            return cause;
        }
    }
}

