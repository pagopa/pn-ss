package it.pagopa.pnss.repositoryManager.model;


import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBDocument;


import it.pagopa.pn.template.rest.v1.dto.ConfidentialityLevel;
import it.pagopa.pn.template.rest.v1.dto.DocumentTypeConfiguration.ChecksumEnum;
import it.pagopa.pn.template.rest.v1.dto.DocumentTypeConfiguration.TimestampedEnum;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

@DynamoDbBean
public class DocTypesEntity {
	
	private String name;
	private String lifeCycleTag;
	private ConfidentialityLevel informationClassification;
	private Boolean digitalSignature;
	private TimestampedEnum timeStamped;
	private ChecksumEnum checkSum;

	
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


	public ConfidentialityLevel getInformationClassification() {
		return informationClassification;
	}


	public void setInformationClassification(ConfidentialityLevel informationClassification) {
		this.informationClassification = informationClassification;
	}


	public Boolean getDigitalSignature() {
		return digitalSignature;
	}


	public void setDigitalSignature(Boolean digitalSignature) {
		this.digitalSignature = digitalSignature;
	}


	public TimestampedEnum getTimeStamped() {
		return timeStamped;
	}


	public void setTimeStamped(TimestampedEnum timeStamped) {
		this.timeStamped = timeStamped;
	}


	public ChecksumEnum getCheckSum() {
		return checkSum;
	}


	public void setCheckSum(ChecksumEnum checkSum) {
		this.checkSum = checkSum;
	}


	public void setName(String name) {
		this.name = name;
	}
	
	
}
