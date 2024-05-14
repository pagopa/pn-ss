package it.pagopa.pn.library.sign.pojo;

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
public class IdentitySecretTimeMark {

    @JsonProperty("user")
    String userTimeMark;

    @JsonProperty("password")
    String passwordTimeMark;
}
