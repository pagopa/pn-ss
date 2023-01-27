package it.pagopa.pnss.repositoryManager.service;

import java.util.List;

import it.pagopa.pn.template.internal.rest.v1.dto.DocumentType;
import it.pagopa.pnss.repositoryManager.dto.DocTypesOutput;

public interface DocTypesService {
	
	DocumentType getDocType(String typeId);
	List<DocTypesOutput> getAllDocType();
	DocumentType insertDocType(DocumentType docTypesInput);
	DocumentType updateDocType(String typeId, DocumentType DocumentType);
	void deleteDocType(String typeId);

}
