package it.pagopa.pnss.repositoryManager.dto;

public class DocTypesInput {

	private String name;
	private String lifeCycleTag;
	private ConfidentialityLevelEnum informationClassification;
	private Boolean digitalSignature;
	private TimestampedEnumDTO timeStamped;
	private ChecksumEnumDTO checkSum;
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
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
	@Override
	public String toString() {
		return "DocTypesInput [name=" + name + ", lifeCycleTag=" + lifeCycleTag + ", informationClassification="
				+ informationClassification + ", digitalSignature=" + digitalSignature + ", timeStamped=" + timeStamped
				+ ", checkSum=" + checkSum + "]";
	}
	
	
	
}
