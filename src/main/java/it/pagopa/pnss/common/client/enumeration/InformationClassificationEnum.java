package it.pagopa.pnss.common.client.enumeration;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * * `C` - Confidential * `HC`- Highly Confidential See [information
 * classification](https://pagopa.atlassian.net/wiki/spaces/EN/pages/357204284/Data+Classification+Handling#Classificazione)
 */
public enum InformationClassificationEnum {

	C("C"),
	HC("HC");

	private String value;

	InformationClassificationEnum(String value) {
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
	public static InformationClassificationEnum fromValue(String value) {
		for (InformationClassificationEnum b : InformationClassificationEnum.values()) {
			if (b.value.equals(value)) {
				return b;
			}
		}
		throw new IllegalArgumentException("Unexpected value '" + value + "'");
	}
}
