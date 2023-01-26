package it.pagopa.pnss.repositoryManager.service;

import java.util.List;

import it.pagopa.pnss.repositoryManager.dto.DocTypesInput;
import it.pagopa.pnss.repositoryManager.dto.DocTypesOutput;

public interface DocTypesService {
	
	DocTypesOutput getDocType(String checkSum);
	List<DocTypesOutput> getAllDocType();
	DocTypesOutput postDocTypes(DocTypesInput docTypesInput);
	DocTypesOutput updateDocTypes(DocTypesInput docTypesInput);
	DocTypesOutput deleteDocTypes(String checkSum);

}
