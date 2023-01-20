package it.pagopa.pnss.repositoryManager.model;


import it.pagopa.pnss.repositoryManager.dto.ChecksumEnumDTO;
import it.pagopa.pnss.repositoryManager.dto.ConfidentialityLevelEnum;
import it.pagopa.pnss.repositoryManager.dto.TimestampedEnumDTO;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

@DynamoDbBean
public class DocTypesEntity {
	
	private String name;
	private String lifeCycleTag;
	private ConfidentialityLevelEnum informationClassification;
	private Boolean digitalSignature;
	private TimestampedEnumDTO timeStamped;
	private ChecksumEnumDTO checkSum;

	
	@DynamoDbPartitionKey
	public String getName() {
		return name;
	}


	public String getLifeCycleTag() {
		return lifeCycleTag;
	}


	public void setLifeCycleTag(String lifeCycleTag) {
		this.lifeCycleTag = lifeCycleTag;
	}


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


	public TimestampedEnumDTO getTimeStamped() {
		return timeStamped;
	}


	public void setTimeStamped(TimestampedEnumDTO timeStamped) {
		this.timeStamped = timeStamped;
	}


	public ChecksumEnumDTO getCheckSum() {
		return checkSum;
	}


	public void setCheckSum(ChecksumEnumDTO checkSum) {
		this.checkSum = checkSum;
	}


	public void setName(String name) {
		this.name = name;
	}


	
}
