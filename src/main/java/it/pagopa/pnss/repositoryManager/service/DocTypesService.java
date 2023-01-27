package it.pagopa.pnss.repositoryManager.service;

import java.util.List;

import it.pagopa.pn.template.internal.rest.v1.dto.DocumentType;
import it.pagopa.pnss.repositoryManager.dto.DocTypesOutput;

public interface DocTypesService {
	
	DocumentType getDocType(String typeId);
	List<DocTypesOutput> getAllDocType();
	DocumentType insertDocType(DocumentType docTypeInput);
	DocumentType updateDocType(String typeId, DocumentType docType);
	void deleteDocType(String typeId);

}
