package it.pagopa.pnss.transformation.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.util.ArrayList;

public class S3ObjectCreated implements Serializable {

    private String version;
    private String id;
    @JsonProperty("detail-type")
    private String detailtype;
    private String source;
    private String account;
    private String time;
    private String region;
    ArrayList< String > resources ;

    @JsonProperty("detail")
    Detail detailObject;


    public S3ObjectCreated(String version, String id, String detailtype, String source, String account, String time, String region, ArrayList<String> resources, Detail detailObject) {
        this.version = version;
        this.id = id;
        this.detailtype = detailtype;
        this.source = source;
        this.account = account;
        this.time = time;
        this.region = region;
        this.resources = resources;
        this.detailObject = detailObject;
    }

    public S3ObjectCreated() {
    }

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
        return detailObject;
    }

    public void setDetailObject(Detail detailObject) {
        this.detailObject = detailObject;
    }

    @Override
    public String toString() {
        return "S3ObjectCreated{" +
                "version='" + version + '\'' +
                ", id='" + id + '\'' +
                ", detailtype='" + detailtype + '\'' +
                ", source='" + source + '\'' +
                ", account='" + account + '\'' +
                ", time='" + time + '\'' +
                ", region='" + region + '\'' +
                ", resources=" + resources +
                ", detailObject=" + detailObject +
                '}';
    }
}
