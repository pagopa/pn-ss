package it.pagopa.pnss.repositoryManager.dto;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBDocument;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;

@DynamoDbBean
public class UserConfigurationDestinationDTO {

	 private String sqsUrl;

	public String getSqsUrl() {
		return sqsUrl;
	}

	public void setSqsUrl(String sqsUrl) {
		this.sqsUrl = sqsUrl;
	}

	@Override
	public String toString() {
		return "UserConfigurationDestinationDTO [sqsUrl=" + sqsUrl + "]";
	}
	 
	 	 
}
