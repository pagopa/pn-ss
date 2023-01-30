package it.pagopa.pnss.repositoryManager.service;

import it.pagopa.pn.template.internal.rest.v1.dto.Document;

public interface DocumentService {
	
	Document getDocument(String documentKey);
	Document insertDocument(Document documentInput);
	Document patchDocument(String documentKey, Document documentInput);
	void deleteDocument(String documentKey);

}
