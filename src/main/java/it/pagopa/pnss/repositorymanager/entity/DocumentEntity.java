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
	/** Stato tecnico */
	private String documentState; // modificabile in POST
	/** Stato logico */
	private String documentLogicalState; // modificabile in POST
	/** timestamp scadenza documento: info calcolata in fase di creazione dell'oggetto da collocare nel bucket hot
	 *  e in caso di variazione dello stato tecnico in 'available' (se info retentionUntil non impostata precedentemente). </br>
	 *  Nota: dipendenza dal tag ?? */
	private String retentionUntil; // modificabile in POST
	private String checkSum; // modificabile in POST
	private String clientShortCode;
	private BigDecimal contentLenght; // modificabile in POST
	private String contentType; 
	private DocTypeEntity documentType;

	@DynamoDbPartitionKey
	public String getDocumentKey() {
		return documentKey;
	}
	
}
