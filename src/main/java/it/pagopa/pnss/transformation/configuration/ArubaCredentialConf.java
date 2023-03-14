package it.pagopa.pnss.transformation.configuration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pnss.transformation.model.pojo.ArubaSecretValue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;

@Configuration
public class ArubaCredentialConf {

    @Autowired
    private SecretsManagerClient secretsManagerClient;
    @Autowired
    private ObjectMapper objectMapper;

    @Value("${ArubaDelegatedDomain:#{null}}")
    public String delegatedDomain;
    @Value("${ArubaDelegatedPassword:#{null}}")
    public String delegatedPassword;
    @Value("${ArubaDelegatedUser:#{null}}")
    public String delegatedUser;
    @Value("${ArubaOtpPwd:#{null}}")
    public String otpPwd;
    @Value("${ArubaTypeOtpAuth:#{null}}")
    public String typeOtpAuth;

    @Value("${ArubaUser:#{null}}")
    public String user;

    @Bean
    public ArubaSecretValue arubaCredentialProvider() {
        try {
            if (delegatedDomain != null && delegatedPassword != null && delegatedUser != null && otpPwd != null && typeOtpAuth != null && user != null) {
                return new ArubaSecretValue(delegatedDomain, delegatedUser, delegatedPassword, otpPwd, typeOtpAuth, user);
            } else {
                String secretStringJson = secretsManagerClient.getSecretValue(builder -> builder.secretId("pn/identity/signature")).secretString();
                return objectMapper.readValue(secretStringJson, ArubaSecretValue.class);
            }
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}