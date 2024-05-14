package it.pagopa.pnss.common.client.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Getter
public class ScadenzaDocumentiCallException extends RuntimeException {

    private int code;
    private String message;

}
