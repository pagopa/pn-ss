package it.pagopa.pnss.repositorymanager.entity;

import java.util.List;

import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.ToString;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

@DynamoDbBean
@Data
@ToString
public class UserConfigurationEntity {
	
	@Getter(AccessLevel.NONE)
	private String name;
	private List<String> canCreate;
	private List<String> canRead;
	private String signatureInfo;
	private UserConfigurationDestinationEntity destination; 
	@Getter(AccessLevel.NONE)
	private String apiKey;
	
	@DynamoDbPartitionKey
	public String getName() {
		return name;
	}
	
//	@DynamoDbSortKey
	public String getApiKey() {
		return apiKey;
	}



}
