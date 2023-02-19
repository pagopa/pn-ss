package it.pagopa.pnss.transformation.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;

public class S3ObjectCreated {

    private String version;
    private String id;
    @JsonProperty("detail-type")
    private String detailtype;
    private String source;
    private String account;
    private String time;
    private String region;
    ArrayList< String > resources ;
    Detail DetailObject;
    Oggetto object;
    @JsonProperty("request-id")
    String requestId;
    String requester;
    @JsonProperty("source-ip-address")
    String sourceIpAddress;
    String reason;

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDetailtype() {
        return detailtype;
    }

    public void setDetailtype(String detailtype) {
        this.detailtype = detailtype;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public ArrayList<String> getResources() {
        return resources;
    }

    public void setResources(ArrayList<String> resources) {
        this.resources = resources;
    }

    public Detail getDetailObject() {
        return DetailObject;
    }

    public void setDetailObject(Detail detailObject) {
        DetailObject = detailObject;
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
