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
	private String name; // ok
	private List<String> canCreate; // ok
	private List<String> canRead; // ok
	/** __DA DEFINIRE__ configurazioni necessarie per la firma digitale */
	private String signatureInfo; // da verificare il tipo
	private UserConfigurationDestinationEntity destination;  // ok
	private String apiKey; // ok
	
	@DynamoDbPartitionKey
	public String getName() {
		return name;
	}


}
