package it.pagopa.pnss.transformation.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Detail {
    private String version;
    Bucket BucketObject;
    Object ObjectObject;
    @JsonProperty("request-id")
    private String requestId;
    private String requester;
    @JsonProperty("source-ip-address")
    private String sourceIpAddress;
    private String reason;

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public Bucket getBucketObject() {
        return BucketObject;
    }

    public void setBucketObject(Bucket bucketObject) {
        BucketObject = bucketObject;
    }

    public Object getObjectObject() {
        return ObjectObject;
    }

    public void setObjectObject(Object objectObject) {
        ObjectObject = objectObject;
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
