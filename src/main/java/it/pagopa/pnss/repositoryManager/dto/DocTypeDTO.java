package it.pagopa.pnss.repositoryManager.dto;

import it.pagopa.pnss.repositoryManager.enumeration.ChecksumEnum;
import it.pagopa.pnss.repositoryManager.enumeration.ConfidentialityLevelEnum;
import it.pagopa.pnss.repositoryManager.enumeration.TimestampedEnum;
import it.pagopa.pnss.repositoryManager.enumeration.TipoDocumentoEnum;
import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class DocTypeDTO {
	
	private TipoDocumentoEnum tipoDocumento;
	private ChecksumEnum checkSum;
	private String lifeCycleTag;
	private String tipoTrasformazione;
	private ConfidentialityLevelEnum informationClassification;
	private Boolean digitalSignature;
	private TimestampedEnum timeStamped;

}
