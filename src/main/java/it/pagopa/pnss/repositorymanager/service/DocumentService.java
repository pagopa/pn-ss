package it.pagopa.pnss.repositorymanager.service;

import it.pagopa.pn.template.internal.rest.v1.dto.Document;
import it.pagopa.pn.template.internal.rest.v1.dto.DocumentChanges;
import it.pagopa.pn.template.internal.rest.v1.dto.DocumentInput;
import reactor.core.publisher.Mono;

public interface DocumentService {
	
	Mono<Document> getDocument(String documentKey);
	Mono<Document> insertDocument(DocumentInput documentInput);
	Mono<Document> patchDocument(String documentKey, DocumentChanges documentChanges, String authPagopaSafestorageCxId, String authApiKey);
	Mono<Document> deleteDocument(String documentKey);

}
