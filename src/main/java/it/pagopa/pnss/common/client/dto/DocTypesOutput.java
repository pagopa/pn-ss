package it.pagopa.pnss.common.client.dto;

import it.pagopa.pn.template.internal.rest.v1.dto.DocumentType.TypeIdEnum;
import it.pagopa.pnss.common.client.enumeration.ChecksumEnum;
import it.pagopa.pnss.common.client.enumeration.InformationClassificationEnum;
import it.pagopa.pnss.common.client.enumeration.TimeStampedEnum;
import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class DocTypesOutput {

	private TypeIdEnum typeId;
	private ChecksumEnum checkSum;
	private String lifeCycleTag;
	private String tipoTrasformazione;
	private InformationClassificationEnum informationClassification;
	private Boolean digitalSignature;
	private TimeStampedEnum timeStamped;
	
}
