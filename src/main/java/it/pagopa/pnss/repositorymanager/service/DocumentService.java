package it.pagopa.pnss.repositorymanager.service;

import it.pagopa.pn.template.internal.rest.v1.dto.Document;
import it.pagopa.pn.template.internal.rest.v1.dto.DocumentChanges;
import reactor.core.publisher.Mono;

public interface DocumentService {
	
	Mono<Document> getDocument(String documentKey);
	Mono<Document> insertDocument(Document documentInput);
	Mono<Document> patchDocument(String documentKey, DocumentChanges documentChanges);
	Mono<Document> deleteDocument(String documentKey);

}
