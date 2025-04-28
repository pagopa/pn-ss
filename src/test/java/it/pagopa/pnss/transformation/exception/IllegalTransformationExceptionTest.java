package it.pagopa.pnss.transformation.exception;

import it.pagopa.pnss.testutils.annotation.SpringBootTestWebEnv;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTestWebEnv
public class IllegalTransformationExceptionTest {

    @Test
    public void testIllegalTransformationExceptionMessage() {
        String fileKey = "document123";

        IllegalTransformationException exception = assertThrows(IllegalTransformationException.class, () -> {
            throw new IllegalTransformationException(fileKey);
        });

        String expectedMessage = String.format("Document '%s' does not have a valid transformation type", fileKey);
        assertEquals(expectedMessage, exception.getMessage());
    }

}
