package it.pagopa.pnss.transformation.configuration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pnss.common.exception.JsonSecretFindingException;
import it.pagopa.pnss.transformation.model.pojo.ArubaSecretValue;
import it.pagopa.pnss.transformation.model.pojo.IdentitySecretTimeMark;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;

@Configuration
@Slf4j
public class ArubaCredentialConf {

    private final SecretsManagerClient secretsManagerClient;
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

    public ArubaCredentialConf(SecretsManagerClient secretsManagerClient, ObjectMapper objectMapper) {
        this.secretsManagerClient = secretsManagerClient;
        this.objectMapper = objectMapper;
    }

    @Bean
    public ArubaSecretValue arubaCredentialProvider() {

        try {
            if (delegatedDomain != null && delegatedPassword != null && delegatedUser != null && otpPwd != null && typeOtpAuth != null &&
                user != null) {
                ArubaSecretValue arubaSecretValue =
                        new ArubaSecretValue(delegatedDomain, delegatedUser, delegatedPassword, otpPwd, typeOtpAuth, user);
                return arubaSecretValue;
            } else {
                String secretStringJson =
                        secretsManagerClient.getSecretValue(builder -> builder.secretId("pn/identity/signature")).secretString();
                ArubaSecretValue arubaSecretValue = objectMapper.readValue(secretStringJson, ArubaSecretValue.class);
                return arubaSecretValue;
            }
        } catch (JsonProcessingException e) {
            throw new JsonSecretFindingException(ArubaSecretValue.class);
        }
    }

    @Bean
    public IdentitySecretTimeMark identityTimeMarkProvider() {

        try {
            if (userTimeMark != null && passwordTimeMark != null) {
                IdentitySecretTimeMark identitySecretTimemark = new IdentitySecretTimeMark(userTimeMark, passwordTimeMark);
                return identitySecretTimemark;
            } else {
                String secretStringJson =
                        secretsManagerClient.getSecretValue(builder -> builder.secretId("pn/identity/timemark")).secretString();
                IdentitySecretTimeMark identitySecretTimemark = objectMapper.readValue(secretStringJson, IdentitySecretTimeMark.class);
                return identitySecretTimemark;
            }
        } catch (JsonProcessingException e) {
            throw new JsonSecretFindingException(IdentitySecretTimeMark.class);
        }
    }
}