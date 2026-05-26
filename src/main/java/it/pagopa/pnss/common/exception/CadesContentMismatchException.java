package it.pagopa.pnss.common.exception;

import it.pagopa.pn.library.exceptions.PnSpapiTemporaryErrorException;

public class CadesContentMismatchException extends PnSpapiTemporaryErrorException {

    public CadesContentMismatchException(String fileKey) {
        super(String.format("CAdES content hash mismatch for file: %s", fileKey));
    }
}
