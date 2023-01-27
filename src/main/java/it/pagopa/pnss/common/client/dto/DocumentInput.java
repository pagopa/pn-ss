package it.pagopa.pnss.common.client.dto;

import it.pagopa.pnss.common.client.enumeration.DocumentStateEnum;
import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class DocumentInput {

	private String documentKey;
	private DocumentStateEnum documentState;
	private String retentionPeriod;
	private String checkSum;
	private String contentLenght;
	private String contentType;
	private String documentType;
	
}
