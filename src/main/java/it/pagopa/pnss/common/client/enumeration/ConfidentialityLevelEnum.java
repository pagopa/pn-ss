package it.pagopa.pnss.common.client.enumeration;


import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTypeConvertedEnum;
import com.fasterxml.jackson.annotation.JsonValue;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;

@DynamoDBTypeConvertedEnum
public enum ConfidentialityLevelEnum {
	
	  C("C"),
	  
	  HC("HC");

	  private String value;
	  
	  ConfidentialityLevelEnum(String value) {
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
