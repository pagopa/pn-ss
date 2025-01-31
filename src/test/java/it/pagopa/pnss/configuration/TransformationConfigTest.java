package it.pagopa.pnss.configuration;

import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.DocumentType;
import it.pagopa.pnss.testutils.annotation.SpringBootTestWebEnv;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTestWebEnv
class TransformationConfigTest {
    @Autowired
    private TransformationConfig transformationConfig;

    @ParameterizedTest
    @EnumSource(value = DocumentType.TransformationsEnum.class, names = {"NONE"}, mode = EnumSource.Mode.EXCLUDE)
    void getTransformationQueueName_Ok(DocumentType.TransformationsEnum transformation) {
        String queueName = transformationConfig.getTransformationQueueName(transformation);
        assertNotNull(queueName);
        assertFalse(queueName.isEmpty());
    }

    @Test
    void getTransformationQueueName_None() {
        String queueName = transformationConfig.getTransformationQueueName(DocumentType.TransformationsEnum.NONE);
        assertNull(queueName);
    }

}
