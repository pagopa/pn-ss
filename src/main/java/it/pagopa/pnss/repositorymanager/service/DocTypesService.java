package it.pagopa.pnss.repositorymanager.service;

import java.util.List;

import it.pagopa.pn.template.internal.rest.v1.dto.DocumentType;
import reactor.core.publisher.Mono;

public interface DocTypesService {
	
	Mono<DocumentType> getDocType(String typeId);
	//TODO sistemare
	Mono<List<DocumentType>> getAllDocumentType();
	Mono<DocumentType> insertDocType(DocumentType docTypeInput);
	Mono<DocumentType> updateDocType(String typeId, DocumentType docType);
	Mono<DocumentType> deleteDocType(String typeId);

}
