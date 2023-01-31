package it.pagopa.pnss.common.client.enumeration;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum TipoDocumentoEnum {
	
    NOTIFICATION_ATTACHMENTS("PN_NOTIFICATION_ATTACHMENTS"),
    AAR("PN_AAR"),
    LEGAL_FACTS("PN_LEGAL_FACTS"),
    EXTERNAL_LEGAL_FACTS("PN_EXTERNAL_LEGAL_FACTS");

    private String value;

    TipoDocumentoEnum(String value) {
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
    public static TipoDocumentoEnum fromValue(String value) {
      for (TipoDocumentoEnum b : TipoDocumentoEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }

}
