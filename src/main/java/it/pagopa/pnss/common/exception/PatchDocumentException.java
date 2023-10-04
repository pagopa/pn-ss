package it.pagopa.pnss.common.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.http.HttpStatus;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class PatchDocumentException extends RuntimeException {

    private String message;
    private HttpStatus statusCode;

}
