package it.pagopa.pnss.repositoryManager.dto;

import it.pagopa.pnss.repositoryManager.model.DocTypesEntity;

public class DocTypesOutput {

	private DocTypesEntity documentType;

	public DocTypesEntity getDocumentType() {
		return documentType;
	}

	public void setDocumentType(DocTypesEntity documentType) {
		this.documentType = documentType;
	}
}
