package it.pagopa.pnss.repositorymanager.service;

import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.DocumentChanges;
import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.DocumentInput;
import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.DocumentResponseDocument;
import reactor.core.publisher.Mono;

public interface DocumentService {
	
	Mono<DocumentResponseDocument> getDocument(String documentKey);
	Mono<DocumentResponseDocument> insertDocument(DocumentInput documentInput);
	Mono<DocumentResponseDocument> patchDocument(String documentKey, DocumentChanges documentChanges, String authPagopaSafestorageCxId, String authApiKey);
	Mono<DocumentResponseDocument> deleteDocument(String documentKey);

}
