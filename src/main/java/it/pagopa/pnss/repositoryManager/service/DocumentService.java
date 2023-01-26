package it.pagopa.pnss.repositoryManager.service;

import it.pagopa.pnss.repositoryManager.dto.DocumentInput;
import it.pagopa.pnss.repositoryManager.dto.DocumentOutput;

public interface DocumentService {
	
	DocumentOutput getDocument(String documentKey);
	DocumentOutput postdocument(DocumentInput documentInput);
	DocumentOutput updatedocument(DocumentInput documentInput);
	DocumentOutput deletedocument(String documentKey);

}
