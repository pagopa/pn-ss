package it.pagopa.pnss.repositoryManager.service;

import it.pagopa.pn.template.internal.rest.v1.dto.Document;
import reactor.core.publisher.Mono;

public interface DocumentService {
	
	Mono<Document> getDocument(String documentKey);
	Mono<Document> insertDocument(Document documentInput);
	Mono<Document> patchDocument(String documentKey, Document documentInput);
	Mono<Document> deleteDocument(String documentKey);

}
