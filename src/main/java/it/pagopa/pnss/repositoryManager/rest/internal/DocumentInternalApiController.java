package it.pagopa.pnss.repositoryManager.rest.internal;

import static org.springframework.http.HttpStatus.OK;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;

import it.pagopa.pn.template.internal.rest.v1.api.DocumentInternalApi;
import it.pagopa.pn.template.internal.rest.v1.dto.Document;
import it.pagopa.pnss.repositoryManager.service.DocumentService;
import reactor.core.publisher.Mono;

@RestController
public class DocumentInternalApiController implements DocumentInternalApi {

	@Autowired
	private DocumentService documentService;
	
	@Override
    public Mono<ResponseEntity<Document>> getDocument(String documentKey,  final ServerWebExchange exchange) {

    	Document document = documentService.getDocument(documentKey);
    	return Mono.just(ResponseEntity.ok().body(document));

    }

	@Override
    public  Mono<ResponseEntity<Document>> insertDocument(Mono<Document> document,  final ServerWebExchange exchange) {

    	return document.map(request -> {
    		Document documentInserted = documentService.insertDocument(request);
    		return ResponseEntity.ok().body(documentInserted);
    	});

    }

	@Override
    public Mono<ResponseEntity<Document>> patchDoc(String documentKey, Mono<Document> document,  final ServerWebExchange exchange) {
    	
    	return document.map(request -> {
    		Document documentInserted = documentService.patchDocument(documentKey, request);
    		return ResponseEntity.ok().body(documentInserted);
    	});

    }

	@Override
    public Mono<ResponseEntity<Void>> deleteDocument(String documentKey,  final ServerWebExchange exchange) {
		
		documentService.deleteDocument(documentKey);
    	return Mono.just(new ResponseEntity<>(OK));	
    	
    }

}
