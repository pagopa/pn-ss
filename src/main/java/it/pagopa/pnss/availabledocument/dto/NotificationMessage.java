package it.pagopa.pnss.availabledocument.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

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

    public NotificationMessage() {
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getVersionId() {
        return versionId;
    }

    public void setVersionId(String versionId) {
        this.versionId = versionId;
    }

    public String getDocumentType() {
        return documentType;
    }

    public void setDocumentType(String documentType) {
        this.documentType = documentType;
    }

    public String getDocumentStatus() {
        return documentStatus;
    }

    public void setDocumentStatus(String documentStatus) {
        this.documentStatus = documentStatus;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getChecksum() {
        return checksum;
    }

    public void setChecksum(String checksum) {
        this.checksum = checksum;
    }

    public String getRetentionUntil() {
        return retentionUntil;
    }

    public void setRetentionUntil(String retentionUntil) {
        this.retentionUntil = retentionUntil;
    }

    public String getClientShortCode() {
        return clientShortCode;
    }

    public void setClientShortCode(String clientShortCode) {
        this.clientShortCode = clientShortCode;
    }

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
