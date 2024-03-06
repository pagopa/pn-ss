package it.pagopa.pn.library.sign.configuration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pnss.common.exception.JsonSecretFindingException;
import it.pagopa.pn.library.sign.pojo.ArubaSecretValue;
import it.pagopa.pn.library.sign.pojo.IdentitySecretTimeMark;
import lombok.CustomLog;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;

@Configuration
@CustomLog
public class ArubaCredentialConf {

    private final SecretsManagerClient smClient;
    private final ObjectMapper objectMapper;

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

    @Value("${TimemarkUser:#{null}}")
    public String userTimeMark;

    @Value("${TimemarkPassword:#{null}}")
    public String passwordTimeMark;

    public ArubaCredentialConf(SecretsManagerClient smClient, ObjectMapper objectMapper) {
        this.smClient = smClient;
        this.objectMapper = objectMapper;
    }

    @Bean
    public ArubaSecretValue arubaCredentialProvider() {

        try {
            if (delegatedDomain != null && delegatedPassword != null && delegatedUser != null && otpPwd != null && typeOtpAuth != null &&
                user != null) {
                return new ArubaSecretValue(delegatedDomain, delegatedUser, delegatedPassword, otpPwd, typeOtpAuth, user);
            } else {
                String secretStringJson =
                        smClient.getSecretValue(builder -> builder.secretId("pn/identity/signature")).secretString();
                return objectMapper.readValue(secretStringJson, ArubaSecretValue.class);
            }
        } catch (JsonProcessingException e) {
            throw new JsonSecretFindingException(ArubaSecretValue.class);
        }
    }

    @Bean
    public IdentitySecretTimeMark identityTimeMarkProvider() {

        try {
            if (userTimeMark != null && passwordTimeMark != null) {
                return new IdentitySecretTimeMark(userTimeMark, passwordTimeMark);
            } else {
                String secretStringJson =
                        smClient.getSecretValue(builder -> builder.secretId("pn/identity/timemark")).secretString();
                return objectMapper.readValue(secretStringJson, IdentitySecretTimeMark.class);
            }
        } catch (JsonProcessingException e) {
            throw new JsonSecretFindingException(IdentitySecretTimeMark.class);
        }
    }
}