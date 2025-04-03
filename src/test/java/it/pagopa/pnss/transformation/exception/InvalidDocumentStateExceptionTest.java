package it.pagopa.pnss.transformation.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class InvalidDocumentStateExceptionTest {

    @Test
    public void testInvalidDocumentStateExceptionMessage() {
        String documentState = "Draft";
        String fileKey = "document123";

        InvalidDocumentStateException exception = assertThrows(InvalidDocumentStateException.class, () -> {
            throw new InvalidDocumentStateException(documentState, fileKey);
        });

        String expectedMessage = String.format("Status '%s' is not valid for transformation for document '%s'", documentState, fileKey);
        assertEquals(expectedMessage, exception.getMessage());
    }

}