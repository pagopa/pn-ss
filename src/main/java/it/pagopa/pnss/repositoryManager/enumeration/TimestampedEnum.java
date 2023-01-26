package it.pagopa.pnss.repositoryManager.enumeration;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTypeConvertedEnum;
import com.fasterxml.jackson.annotation.JsonValue;

@DynamoDBTypeConvertedEnum
public enum TimestampedEnum {

	NONE("NONE"), STANDARD("STANDARD");

	private String value;

	TimestampedEnum(String value) {
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
