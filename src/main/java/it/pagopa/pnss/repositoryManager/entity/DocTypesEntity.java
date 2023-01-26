package it.pagopa.pnss.repositoryManager.entity;


import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTypeConvertedEnum;

import it.pagopa.pnss.repositoryManager.dto.ChecksumEnumDTO;
import it.pagopa.pnss.repositoryManager.dto.ConfidentialityLevelEnum;
import it.pagopa.pnss.repositoryManager.dto.TimestampedEnumDTO;
import it.pagopa.pnss.repositoryManager.dto.TipoDocumentoEnum;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

@DynamoDbBean
public class DocTypesEntity {
	
	private TipoDocumentoEnum tipoDocumento;
	private ChecksumEnumDTO checkSum;
	private String lifeCycleTag;
	private String tipoTrasformazione;
	private ConfidentialityLevelEnum informationClassification;
	private Boolean digitalSignature;
	private TimestampedEnumDTO timeStamped;
	
	@DynamoDbPartitionKey
	@DynamoDBTypeConvertedEnum
	public TipoDocumentoEnum getTipoDocumento() {
		return tipoDocumento;
	}
	public void setTipoDocumento(TipoDocumentoEnum tipoDocumento) {
		this.tipoDocumento = tipoDocumento;
	}

	public ChecksumEnumDTO getCheckSum() {
		return checkSum;
	}
	public void setCheckSum(ChecksumEnumDTO checkSum) {
		this.checkSum = checkSum;
	}
	
	public String getLifeCycleTag() {
		return lifeCycleTag;
	}
	public void setLifeCycleTag(String lifeCycleTag) {
		this.lifeCycleTag = lifeCycleTag;
	}

	@DynamoDBTypeConvertedEnum
	public ConfidentialityLevelEnum getInformationClassification() {
		return informationClassification;
	}
	public void setInformationClassification(ConfidentialityLevelEnum informationClassification) {
		this.informationClassification = informationClassification;
	}

	public Boolean getDigitalSignature() {
		return digitalSignature;
	}
	public void setDigitalSignature(Boolean digitalSignature) {
		this.digitalSignature = digitalSignature;
	}

	@DynamoDBTypeConvertedEnum
	public TimestampedEnumDTO getTimeStamped() {
		return timeStamped;
	}
	public void setTimeStamped(TimestampedEnumDTO timeStamped) {
		this.timeStamped = timeStamped;
	}
	
	public String getTipoTrasformazione() {
		return tipoTrasformazione;
	}
	public void setTipoTrasformazione(String tipoTrasformazione) {
		this.tipoTrasformazione = tipoTrasformazione;
	}
	
}
