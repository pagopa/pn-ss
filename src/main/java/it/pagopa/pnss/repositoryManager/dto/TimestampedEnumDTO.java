package it.pagopa.pnss.repositoryManager.dto;

import com.fasterxml.jackson.annotation.JsonValue;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;

@DynamoDbBean
public enum TimestampedEnumDTO {
	
	    NONE("NONE"),
	    
	    STANDARD("STANDARD");

	    private String value;

	    TimestampedEnumDTO(String value) {
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


