package it.pagopa.pnss.repositoryManager.dto;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTypeConvertedEnum;
import com.fasterxml.jackson.annotation.JsonValue;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;

@DynamoDBTypeConvertedEnum
public enum ChecksumEnumDTO {
	
    MD5("MD5"),
    
    SHA256("SHA256");

    private String value;

    ChecksumEnumDTO(String value) {
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
