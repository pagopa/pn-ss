package it.pagopa.pnss.common.client.enumeration;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum TimeStampedEnum {
	
    NONE("NONE"),
    STANDARD("STANDARD");

    private String value;

    TimeStampedEnum(String value) {
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

    @JsonCreator
    public static TimeStampedEnum fromValue(String value) {
      for (TimeStampedEnum b : TimeStampedEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }

}
