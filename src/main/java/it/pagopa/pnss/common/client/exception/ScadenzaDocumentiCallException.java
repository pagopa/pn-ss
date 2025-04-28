package it.pagopa.pnss.common.client.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class ScadenzaDocumentiCallException extends RuntimeException {

    private final int code;
    private final String message;

}
