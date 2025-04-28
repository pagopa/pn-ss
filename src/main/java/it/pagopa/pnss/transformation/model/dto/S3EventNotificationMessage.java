package it.pagopa.pnss.transformation.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.ArrayList;

@FieldDefaults(level = AccessLevel.PRIVATE)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class S3EventNotificationMessage {

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
    S3EventNotificationDetail eventNotificationDetail;

}
