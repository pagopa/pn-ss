package it.pagopa.pnss.common.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.springframework.http.HttpStatus;

@AllArgsConstructor
@Getter
@Setter
public class PatchDocumentException extends RuntimeException {

    private final String message;
    private final HttpStatus statusCode;

}
