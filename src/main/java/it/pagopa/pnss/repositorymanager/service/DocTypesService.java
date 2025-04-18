package it.pagopa.pnss.repositorymanager.service;

import java.util.List;

import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.DocumentType;
import reactor.core.publisher.Mono;

public interface DocTypesService {
	
	Mono<DocumentType> getDocType(String typeId);
	Mono<List<DocumentType>> getAllDocumentType();
	Mono<DocumentType> insertDocType(DocumentType docTypeInput);
	Mono<DocumentType> updateDocType(String typeId, DocumentType docType);
	Mono<DocumentType> deleteDocType(String typeId);

}
