package it.pagopa.pnss.repositoryManager.enumeration;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTypeConvertedEnum;
import com.fasterxml.jackson.annotation.JsonValue;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;

@DynamoDBTypeConvertedEnum
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
