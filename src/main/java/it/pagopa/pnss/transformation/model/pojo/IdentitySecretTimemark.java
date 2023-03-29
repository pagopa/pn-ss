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
public class IdentitySecretTimemark {

    @JsonProperty("user")
    String userTimemark;

    @JsonProperty("password")
    String passwordTimemark;

    @Override
    public String toString() {
        return "IdentitySecretTimemark{" +
                "userTimemark='" + userTimemark + "'}";
    }

}
