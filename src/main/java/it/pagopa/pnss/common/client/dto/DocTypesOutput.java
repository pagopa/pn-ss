package it.pagopa.pnss.common.client.dto;

import it.pagopa.pnss.common.client.enumeration.ChecksumEnum;
import it.pagopa.pnss.common.client.enumeration.InformationClassificationEnum;
import it.pagopa.pnss.common.client.enumeration.TimeStampedEnum;
import it.pagopa.pnss.common.client.enumeration.TipoDocumentoEnum;
import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class DocTypesOutput {

	private TipoDocumentoEnum name;
	private ChecksumEnum checkSum;
	private String lifeCycleTag;
	private String tipoTrasformazione;
	private InformationClassificationEnum informationClassification;
	private Boolean digitalSignature;
	private TimeStampedEnum timeStamped;
	
}
