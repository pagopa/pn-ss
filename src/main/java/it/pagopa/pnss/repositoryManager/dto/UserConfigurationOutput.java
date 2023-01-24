package it.pagopa.pnss.repositoryManager.dto;

import java.util.List;

import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.FieldDefaults;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

@FieldDefaults(level = AccessLevel.PRIVATE)
@Data
@ToString
@DynamoDbBean
public class UserConfigurationOutput {

	@Getter(AccessLevel.NONE)
	String name;
	
	List<String> canCreate;
	List<String> canRead;
	String signatureInfo;
	UserConfigurationDestinationDTO destination;
	
	@Getter(AccessLevel.NONE)
	String ApiKey;
	
	@DynamoDbPartitionKey
	public String getName() {
		return name;
	}

	@DynamoDbSortKey
	public String getApiKey() {
		return ApiKey;
	}
	

//	public String getName() {
//		return name;
//	}
//
//	public List<String> getCanCreate() {
//		return canCreate;
//	}
//
//	public void setCanCreate(List<String> canCreate) {
//		this.canCreate = canCreate;
//	}
//
//	public List<String> getCanRead() {
//		return canRead;
//	}
//
//	public void setCanRead(List<String> canRead) {
//		this.canRead = canRead;
//	}
//
//	public String getSignatureInfo() {
//		return signatureInfo;
//	}
//
//	public void setSignatureInfo(String signatureInfo) {
//		this.signatureInfo = signatureInfo;
//	}
//
//	public UserConfigurationDestinationDTO getDestination() {
//		return destination;
//	}
//
//	public void setDestination(UserConfigurationDestinationDTO destination) {
//		this.destination = destination;
//	}
//
//	public String getApiKey() {
//		return ApiKey;
//	}
//
//	public void setApiKey(String apiKey) {
//		ApiKey = apiKey;
//	}
//
//	public void setName(String name) {
//		this.name = name;
//	}

}
