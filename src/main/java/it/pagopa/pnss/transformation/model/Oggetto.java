package it.pagopa.pnss.transformation.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Oggetto {
    private String key;
    private float size;
    private String etag;
    @JsonProperty("version-id")
    private String versionId;
    private String sequencer;

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public float getSize() {
        return size;
    }

    public void setSize(float size) {
        this.size = size;
    }

    public String getEtag() {
        return etag;
    }

    public void setEtag(String etag) {
        this.etag = etag;
    }

    public String getVersionId() {
        return versionId;
    }

    public void setVersionId(String versionId) {
        this.versionId = versionId;
    }

    public String getSequencer() {
        return sequencer;
    }

    public void setSequencer(String sequencer) {
        this.sequencer = sequencer;
    }
}
