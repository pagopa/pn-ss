package it.pagopa.pnss.transformation.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE)
@Data
@NoArgsConstructor
public class CreationDetail {

    String version;
    BucketOriginDetail bucketOriginDetail;
    S3Object object;

    @JsonProperty("request-id")
    String requestId;

    String requester;

    @JsonProperty("source-ip-address")
    String sourceIpAddress;

    String reason;
}
