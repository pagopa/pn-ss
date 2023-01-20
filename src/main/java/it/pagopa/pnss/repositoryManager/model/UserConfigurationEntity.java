package it.pagopa.pnss.repositoryManager.model;

import java.util.List;

import it.pagopa.pnss.repositoryManager.dto.UserConfigurationDestinationDTO;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

@DynamoDbBean
public class UserConfigurationEntity {
	
	private String name;
	private List<String> canCreate;
	private List<String> canRead;
	private String signatureInfo;
	private UserConfigurationDestinationDTO destination; 
	private String ApiKey;
	
	@DynamoDbPartitionKey
	public String getName() {
		return name;
	}

	public List<String> getCanCreate() {
		return canCreate;
	}

	public void setCanCreate(List<String> canCreate) {
		this.canCreate = canCreate;
	}

	public List<String> getCanRead() {
		return canRead;
	}

	public void setCanRead(List<String> canRead) {
		this.canRead = canRead;
	}


	public String getSignatureInfo() {
		return signatureInfo;
	}

	public void setSignatureInfo(String signatureInfo) {
		this.signatureInfo = signatureInfo;
	}

	public UserConfigurationDestinationDTO getDestination() {
		return destination;
	}

	public void setDestination(UserConfigurationDestinationDTO destination) {
		this.destination = destination;
	}

	public String getApiKey() {
		return ApiKey;
	}

	public void setApiKey(String apiKey) {
		ApiKey = apiKey;
	}

	public void setName(String name) {
		this.name = name;
	}
	
	

}
