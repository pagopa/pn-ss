package it.pagopa.pnss.repositoryManager.entity;


import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTypeConvertedEnum;

import it.pagopa.pn.template.internal.rest.v1.dto.DocumentType.ChecksumEnum;
import it.pagopa.pn.template.internal.rest.v1.dto.DocumentType.InformationClassificationEnum;
import it.pagopa.pn.template.internal.rest.v1.dto.DocumentType.TimeStampedEnum;
import it.pagopa.pnss.common.client.enumeration.TipoDocumentoEnum;
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
	private TipoDocumentoEnum tipoDocumento; // ok
	private ChecksumEnum checkSum; // ok
	private String lifeCycleTag; // ok
//	private String tipoTrasformazione; // ko
	@Getter(AccessLevel.NONE)
	private InformationClassificationEnum informationClassification; // ok
	private Boolean digitalSignature; // ok
	@Getter(AccessLevel.NONE)
	private TimeStampedEnum timeStamped; // ok
	
	@DynamoDbPartitionKey
	@DynamoDBTypeConvertedEnum
	public TipoDocumentoEnum getTipoDocumento() {
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
