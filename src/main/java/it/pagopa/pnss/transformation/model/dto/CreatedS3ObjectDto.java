package it.pagopa.pnss.transformation.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.util.ArrayList;

@FieldDefaults(level = AccessLevel.PRIVATE)
@Data
@NoArgsConstructor
public class CreatedS3ObjectDto {

    String version;
    String id;

    @JsonProperty("detail-type")
    String detailType;

    String source;
    String account;
    String time;
    String region;
    ArrayList<String> resources;

    @JsonProperty("detail")
    CreationDetail creationDetailObject;

    int retry;
}
