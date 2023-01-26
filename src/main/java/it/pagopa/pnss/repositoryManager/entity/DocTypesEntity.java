package it.pagopa.pnss.repositoryManager.entity;


import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTypeConvertedEnum;

import it.pagopa.pnss.repositoryManager.enumeration.ChecksumEnum;
import it.pagopa.pnss.repositoryManager.enumeration.ConfidentialityLevelEnum;
import it.pagopa.pnss.repositoryManager.enumeration.TimestampedEnum;
import it.pagopa.pnss.repositoryManager.enumeration.TipoDocumentoEnum;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.ToString;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

@DynamoDbBean
@Data
@ToString
public class DocTypesEntity {
	
	@Getter(AccessLevel.NONE)
	private TipoDocumentoEnum tipoDocumento;
	private ChecksumEnum checkSum;
	private String lifeCycleTag;
	private String tipoTrasformazione;
	@Getter(AccessLevel.NONE)
	private ConfidentialityLevelEnum informationClassification;
	private Boolean digitalSignature;
	@Getter(AccessLevel.NONE)
	private TimestampedEnum timeStamped;
	
	@DynamoDbPartitionKey
	@DynamoDBTypeConvertedEnum
	public TipoDocumentoEnum getTipoDocumento() {
		return tipoDocumento;
	}

	@DynamoDBTypeConvertedEnum
	public ConfidentialityLevelEnum getInformationClassification() {
		return informationClassification;
	}

	@DynamoDBTypeConvertedEnum
	public TimestampedEnum getTimeStamped() {
		return timeStamped;
	}
	
}
