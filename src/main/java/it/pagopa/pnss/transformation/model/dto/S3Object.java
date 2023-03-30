package it.pagopa.pnss.transformation.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE)
@Data
@NoArgsConstructor
public class S3Object {

    String key;
    float size;
    String etag;

    @JsonProperty("version-id")
    String versionId;

    String sequencer;
}
