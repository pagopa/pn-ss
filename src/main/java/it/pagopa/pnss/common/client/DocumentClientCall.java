package it.pagopa.pnss.common.client;

import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.Document;
import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.DocumentChanges;
import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.DocumentInput;
import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.DocumentResponse;
import it.pagopa.pnss.common.client.exception.DocumentKeyNotPresentException;
import it.pagopa.pnss.common.client.exception.DocumentkeyPresentException;
import it.pagopa.pnss.common.client.exception.IdClientNotFoundException;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Mono;

public interface DocumentClientCall {

    Mono<DocumentResponse> getDocument(String keyFile) throws DocumentKeyNotPresentException;
    
    Mono<DocumentResponse> postDocument(DocumentInput documentInput) throws DocumentkeyPresentException;
    
    Mono<DocumentResponse> patchDocument(
    		String authPagopaSafestorageCxId, String authApiKey, 
    		String keyFile, DocumentChanges document) throws DocumentKeyNotPresentException;
    
    ResponseEntity<Document> deleteDocument(String keyFile) throws IdClientNotFoundException;
}
