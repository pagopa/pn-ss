package it.pagopa.pnss.availabledocument.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class NotificationMessage {

    String key;
    String versionId;
    String documentType;
    String documentStatus;
    String contentType;
    String checksum;
    String retentionUntil;
    @JsonProperty("client_short_code")
    String clientShortCode;
    @Override
    public String toString() {
        return "NotificationMessage{" +
                "key='" + key + '\'' +
                ", versionId='" + versionId + '\'' +
                ", documentType='" + documentType + '\'' +
                ", documentStatus='" + documentStatus + '\'' +
                ", contentType='" + contentType + '\'' +
                ", checksum='" + checksum + '\'' +
                ", retentionUntil='" + retentionUntil + '\'' +
                ", clientShortCode='" + clientShortCode + '\'' +
                '}';
    }
}
