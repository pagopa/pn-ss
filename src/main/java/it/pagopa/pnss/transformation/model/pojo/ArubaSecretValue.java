package it.pagopa.pnss.transformation.model.pojo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
@AllArgsConstructor
@Data
public class ArubaSecretValue {

    @JsonProperty("delegated_domain")
    String delegatedDomain;
    @JsonProperty("delegated_user")
    String delegatedUser;
    @JsonProperty("delegated_password")
    String delegatedPassword;
    String otpPwd;
    String typeOtpAuth;
    String user;

    @Override
    public String toString() {
        return "ArubaSecretValue{" +
                "delegatedDomain='" + delegatedDomain + '\'' +
                ", delegatedUser='" + delegatedUser + '\'' +
                ", delegatedPassword='" + "*****" + '\'' +
                ", otpPwd='" + otpPwd + '\'' +
                ", typeOtpAuth='" + typeOtpAuth + '\'' +
                ", user='" + user + '\'' +
                '}';
    }
}
