package it.pagopa.pnss.repositorymanager.entity;

import it.pagopa.pn.template.internal.rest.v1.dto.Document.DocumentStateEnum;
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
	private DocumentStateEnum documentState;
	private String retentionPeriod;
	private String checkSum;
	private String contentLenght;
	private String contentType;
	private String documentType;
	
	@DynamoDbPartitionKey
	public String getDocumentKey() {
		return documentKey;
	}
	
}
