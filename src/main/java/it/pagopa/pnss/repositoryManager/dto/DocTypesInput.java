package it.pagopa.pnss.repositoryManager.dto;

public class DocTypesInput {

//	private String name;
	private String lifeCycleTag;
	private String tipoTrasformazione;
	private ConfidentialityLevelEnum informationClassification;
	private Boolean digitalSignature;
	private TimestampedEnumDTO timeStamped;
	
	// partition key
	private ChecksumEnumDTO checkSum;
	// sort key
	private TipoDocumentoEnum tipoDocumento;
	
//	public String getName() {
//		return name;
//	}
//	public void setName(String name) {
//		this.name = name;
//	}
	
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
	public String getTipoTrasformazione() {
		return tipoTrasformazione;
	}
	public void setTipoTrasformazione(String tipoTrasformazione) {
		this.tipoTrasformazione = tipoTrasformazione;
	}
	public TipoDocumentoEnum getTipoDocumento() {
		return tipoDocumento;
	}
	public void setTipoDocumento(TipoDocumentoEnum tipoDocumento) {
		this.tipoDocumento = tipoDocumento;
	}
	
	@Override
	public String toString() {
		return "DocTypesInput [lifeCycleTag=" + lifeCycleTag + ", tipoTrasformazione=" + tipoTrasformazione
				+ ", informationClassification=" + informationClassification + ", digitalSignature=" + digitalSignature
				+ ", timeStamped=" + timeStamped + ", checkSum=" + checkSum + ", tipoDocumento=" + tipoDocumento + "]";
	}

}
