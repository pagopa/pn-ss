package it.pagopa.pnss.repositorymanager.service;

import it.pagopa.pn.template.internal.rest.v1.dto.DocumentType;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface DocTypesService {
	
	Mono<DocumentType> getDocType(String typeId);
	Flux<DocumentType> getAllDocumentType();
	Mono<DocumentType> insertDocType(DocumentType docTypeInput);
	Mono<DocumentType> updateDocType(String typeId, DocumentType docType);
	Mono<DocumentType> deleteDocType(String typeId);

}
