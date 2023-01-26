package it.pagopa.pnss.repositoryManager.entity;

import java.util.List;

import it.pagopa.pnss.repositoryManager.dto.UserConfigurationDestinationDTO;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.ToString;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

@DynamoDbBean
@Data
@ToString
public class UserConfigurationEntity {
	
	@Getter(AccessLevel.NONE)
	private String name;
	private List<String> canCreate;
	private List<String> canRead;
	private String signatureInfo;
	private UserConfigurationDestinationDTO destination; 
	@Getter(AccessLevel.NONE)
	private String apiKey;
	
	@DynamoDbPartitionKey
	public String getName() {
		return name;
	}
	
	@DynamoDbSortKey
	public String getApiKey() {
		return apiKey;
	}

}
