package it.pagopa.pnss.transformation.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Detail {
    private String version;
    Bucket bucket;
    Oggetto object;
    @JsonProperty("request-id")
    private String requestId;
    private String requester;
    @JsonProperty("source-ip-address")
    private String sourceIpAddress;
    private String reason;

    public Detail(String version, Bucket bucket, Oggetto object, String requestId, String requester, String sourceIpAddress, String reason) {
        this.version = version;
        this.bucket = bucket;
        this.object = object;
        this.requestId = requestId;
        this.requester = requester;
        this.sourceIpAddress = sourceIpAddress;
        this.reason = reason;
    }

    public Detail() {
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public Bucket getBucket() {
        return bucket;
    }

    public void setBucket(Bucket bucket) {
        this.bucket = bucket;
    }

    public Oggetto getObject() {
        return object;
    }

    public void setObject(Oggetto object) {
        this.object = object;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getRequester() {
        return requester;
    }

    public void setRequester(String requester) {
        this.requester = requester;
    }

    public String getSourceIpAddress() {
        return sourceIpAddress;
    }

    public void setSourceIpAddress(String sourceIpAddress) {
        this.sourceIpAddress = sourceIpAddress;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
