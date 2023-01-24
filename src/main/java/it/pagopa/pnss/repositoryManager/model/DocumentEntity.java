package it.pagopa.pnss.repositoryManager.model;

import it.pagopa.pnss.repositoryManager.dto.DocumentStateEnum;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

@DynamoDbBean
public class DocumentEntity {

//	private String documentKey;
	private DocumentStateEnum documentState;
	private String retentionPeriod;
	private String checkSum;
//	private String contentLenght;
//	private String contentType;
//	private String documentType;
	
//	public String getDocumentKey() {
//		return documentKey;
//	}
//	public void setDocumentKey(String documentKey) {
//		this.documentKey = documentKey;
//	}
	public DocumentStateEnum getDocumentState() {
		return documentState;
	}
	public void setDocumentState(DocumentStateEnum documentState) {
		this.documentState = documentState;
	}
	public String getRetentionPeriod() {
		return retentionPeriod;
	}
	public void setRetentionPeriod(String retentionPeriod) {
		this.retentionPeriod = retentionPeriod;
	}
	
	@DynamoDbPartitionKey
	public String getCheckSum() {
		return checkSum;
	}
	public void setCheckSum(String checkSum) {
		this.checkSum = checkSum;
	}
	
//	public String getContentLenght() {
//		return contentLenght;
//	}
//	public void setContentLenght(String contentLenght) {
//		this.contentLenght = contentLenght;
//	}
//	public String getContentType() {
//		return contentType;
//	}
//	public void setContentType(String contentType) {
//		this.contentType = contentType;
//	}
//	public String getDocumentType() {
//		return documentType;
//	}
//	public void setDocumentType(String documentType) {
//		this.documentType = documentType;
//	}
	
}
