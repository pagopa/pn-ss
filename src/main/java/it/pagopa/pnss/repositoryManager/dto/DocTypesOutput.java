package it.pagopa.pnss.repositoryManager.dto;

import it.pagopa.pnss.repositoryManager.enumeration.ChecksumEnum;
import it.pagopa.pnss.repositoryManager.enumeration.ConfidentialityLevelEnum;
import it.pagopa.pnss.repositoryManager.enumeration.TimestampedEnum;
import it.pagopa.pnss.repositoryManager.enumeration.TipoDocumentoEnum;

public class DocTypesOutput {

//	private String name;
	private String lifeCycleTag;
	private String tipoTrasformazione;
	private ConfidentialityLevelEnum informationClassification;
	private Boolean digitalSignature;
	private TimestampedEnum timeStamped;
	private ChecksumEnum checkSum;
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
	public String getTipoTrasformazione() {
		return tipoTrasformazione;
	}
	public void setTipoTrasformazione(String tipoTrasformazione) {
		this.tipoTrasformazione = tipoTrasformazione;
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
	public TipoDocumentoEnum getTipoDocumento() {
		return tipoDocumento;
	}
	public void setTipoDocumento(TipoDocumentoEnum tipoDocumento) {
		this.tipoDocumento = tipoDocumento;
	}
	
	@Override
	public String toString() {
		return "DocTypesOutput [lifeCycleTag=" + lifeCycleTag + ", tipoTrasformazione=" + tipoTrasformazione
				+ ", informationClassification=" + informationClassification + ", digitalSignature=" + digitalSignature
				+ ", timeStamped=" + timeStamped + ", checkSum=" + checkSum + ", tipoDocumento=" + tipoDocumento + "]";
	}
	
}
