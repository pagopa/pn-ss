package it.pagopa.pnss.common.client.enumeration;

import com.fasterxml.jackson.annotation.JsonValue;

public enum ChecksumEnum {
	
    MD5("MD5"),
    SHA256("SHA256");

    private String value;

    ChecksumEnum(String value) {
      this.value = value;
    }

    @JsonValue
    public String getValue() {
      return value;
    }

    @Override
    public String toString() {
      return String.valueOf(value);
    }

}
