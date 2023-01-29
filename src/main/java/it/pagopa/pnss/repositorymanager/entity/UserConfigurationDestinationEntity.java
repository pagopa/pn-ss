package it.pagopa.pnss.repositorymanager.entity;

import lombok.Data;
import lombok.ToString;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;

@DynamoDbBean
@Data
@ToString
public class UserConfigurationDestinationEntity {
	
	  private String sqsUrl;

}
