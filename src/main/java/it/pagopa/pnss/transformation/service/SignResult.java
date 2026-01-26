package it.pagopa.pnss.transformation.service;

import it.pagopa.pn.library.sign.pojo.PnSignDocumentResponse;

public interface SignResult {
    record Success(PnSignDocumentResponse response) implements SignResult {}
    record TemporaryError(Throwable message) implements SignResult {}
    record PermanentError(Throwable message) implements SignResult {}
}

