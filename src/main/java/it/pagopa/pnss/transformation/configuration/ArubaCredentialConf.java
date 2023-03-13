package it.pagopa.pnss.transformation.configuration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pnss.transformation.model.pojo.ArubaSecretValue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;

@Configuration
public class ArubaCredentialConf {

    @Autowired
    private SecretsManagerClient secretsManagerClient;

    @Autowired
    private ObjectMapper objectMapper;

    @Bean
    public ArubaSecretValue arubaCredentialProvider() {
        String secretStringJson = secretsManagerClient.getSecretValue(builder -> builder.secretId("pn/identity/pec")).secretString();
        try {
            return objectMapper.readValue(secretStringJson, ArubaSecretValue.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}