package it.pagopa.pnss.repositoryManager.model;

import java.util.List;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBDocument;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTypeConverted;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTypeConvertedJson;

import it.pagopa.pnss.repositoryManager.dto.UserConfigurationDestinationDTO;
import it.pagopa.pnss.repositoryManager.service.UserConfigurationDestinationDTOConverter;
import lombok.Data;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

@Data
@DynamoDbBean
public class UserConfigurationEntity {
	
	private String name;
	private List<String> canCreate;
	private List<String> canRead;
	private Object signatureInfo;
	private UserConfigurationDestinationDTO destination; 
	private String ApiKey;
	
	@DynamoDbPartitionKey
	public String getName() {
		return name;
	}

}
