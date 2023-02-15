package it.pagopa.pnss.repositorymanager.entity;


import java.util.Map;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTypeConvertedEnum;

import it.pagopa.pn.template.internal.rest.v1.dto.DocumentType.ChecksumEnum;
import it.pagopa.pn.template.internal.rest.v1.dto.DocumentType.InformationClassificationEnum;
import it.pagopa.pn.template.internal.rest.v1.dto.DocumentType.TimeStampedEnum;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.ToString;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

@DynamoDbBean
@Data
@ToString
public class DocTypeEntity {
	
	@Getter(AccessLevel.NONE)
	private String tipoDocumento;
	private ChecksumEnum checksum;
	private String initialStatus;
	private Map<String, CurrentStatusEntity> statuses;
	@Getter(AccessLevel.NONE)
	private InformationClassificationEnum informationClassification;
	private Boolean digitalSignature;
	@Getter(AccessLevel.NONE)
	private TimeStampedEnum timeStamped;
	
	@DynamoDbPartitionKey
	public String getTipoDocumento() {
		return tipoDocumento;
	}

	@DynamoDBTypeConvertedEnum
	public InformationClassificationEnum getInformationClassification() {
		return informationClassification;
	}

	@DynamoDBTypeConvertedEnum
	public TimeStampedEnum getTimeStamped() {
		return timeStamped;
	}
	
}
