package it.pagopa.pnss.configuration;

import com.fasterxml.jackson.core.type.TypeReference;
import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.DocumentType;
import it.pagopa.pnss.common.utils.JsonUtils;
import lombok.CustomLog;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.services.ssm.SsmClient;

import java.util.EnumMap;

import static it.pagopa.pnss.common.utils.LogUtils.INITIALIZING;
import static it.pagopa.pnss.common.utils.LogUtils.TRANSFORMATION_CONFIG;

@Configuration
@CustomLog
public class TransformationConfig {
    private final EnumMap<DocumentType.TransformationsEnum, String> config;

    public TransformationConfig(SsmClient ssmClient, JsonUtils jsonUtils, @Value("${pn.ss.transformation.config.parameter-name}") String parameterName) {
        log.debug(INITIALIZING, TRANSFORMATION_CONFIG);
        config = loadConfig(ssmClient, jsonUtils, parameterName);
    }

    private EnumMap<DocumentType.TransformationsEnum, String> loadConfig(SsmClient ssmClient, JsonUtils jsonUtils, String transformationConfigName) {
        String jsonValue = ssmClient.getParameter(builder -> builder.name(transformationConfigName).build()).parameter().value();
        return jsonUtils.convertJsonStringToObject(jsonValue, new TypeReference<>() {});
    }

    public String getTransformationQueueName(DocumentType.TransformationsEnum transformation) {
        return null;
    }

}
