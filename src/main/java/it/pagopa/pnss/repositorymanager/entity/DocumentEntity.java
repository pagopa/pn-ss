package it.pagopa.pnss.repositorymanager.entity;

import java.math.BigDecimal;


import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.ToString;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

@DynamoDbBean
@Data
@ToString
public class DocumentEntity {

	@Getter(AccessLevel.NONE)
	private String documentKey;
	private String documentState; // ok -> togliere stati // modificabile
	// private String retentionPeriod; ko 
	// retentionUntil (ok): timestamp scadenza documento (info calcolata in fase di creazione e variazione di stato, con dipendenza dal tag)
	private String retentionUntil; // modificabile
	private String checkSum; // modificabile
	private BigDecimal contentLenght; // modificabile
	private String contentType; 
	private DocTypeEntity documentType;
	
	@DynamoDbPartitionKey
	public String getDocumentKey() {
		return documentKey;
	}
	
}
