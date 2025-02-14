package it.pagopa.pnss.transformation.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class S3EventNotificationDetail {

    String version;
    @JsonProperty("bucket")
    S3BucketOriginDetail s3BucketOriginDetail;
    S3Object object;
    @JsonProperty("request-id")
    String requestId;
    String requester;
    @JsonProperty("source-ip-address")
    String sourceIpAddress;
    String reason;
}
