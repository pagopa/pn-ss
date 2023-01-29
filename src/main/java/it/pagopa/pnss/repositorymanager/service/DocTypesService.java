package it.pagopa.pnss.repositorymanager.service;

import java.util.List;

import it.pagopa.pn.template.internal.rest.v1.dto.DocumentType;

public interface DocTypesService {
	
	DocumentType getDocType(String typeId);
	List<DocumentType> getAllDocType();
	DocumentType insertDocType(DocumentType docTypeInput);
	DocumentType updateDocType(String typeId, DocumentType docType);
	void deleteDocType(String typeId);

}
