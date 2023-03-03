package it.pagopa.pnss.common.client;

import it.pagopa.pn.template.internal.rest.v1.dto.Document;
import it.pagopa.pn.template.internal.rest.v1.dto.DocumentChanges;
import it.pagopa.pn.template.internal.rest.v1.dto.DocumentInput;
import it.pagopa.pn.template.internal.rest.v1.dto.DocumentResponse;
import it.pagopa.pnss.common.client.exception.IdClientNotFoundException;

import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Mono;

public interface DocumentClientCall {

    Mono<DocumentResponse> getdocument(String keyFile) throws IdClientNotFoundException;
    Mono<DocumentResponse> postDocument(DocumentInput documentInput) throws IdClientNotFoundException;
    ResponseEntity<Document> updatedocument( Document document) throws IdClientNotFoundException;
    
    Mono<DocumentResponse> patchdocument(
    		String authPagopaSafestorageCxId, String authApiKey, 
    		String keyFile, DocumentChanges document) throws IdClientNotFoundException;
    
    ResponseEntity<Document> deletedocument(String keyFile) throws IdClientNotFoundException;
}
