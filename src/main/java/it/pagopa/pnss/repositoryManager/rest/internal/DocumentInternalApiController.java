package it.pagopa.pnss.repositoryManager.rest.internal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;

import it.pagopa.pn.template.internal.rest.v1.api.DocumentInternalApi;
import it.pagopa.pn.template.internal.rest.v1.dto.Document;
import it.pagopa.pnss.common.client.exception.IdClientNotFoundException;
import it.pagopa.pnss.repositoryManager.exception.ItemAlreadyPresent;
import it.pagopa.pnss.repositoryManager.service.DocumentService;
import reactor.core.publisher.Mono;

@RestController
public class DocumentInternalApiController implements DocumentInternalApi {

	@Autowired
	private DocumentService documentService;
	
	@Override
    public Mono<ResponseEntity<Document>> getDocument(String documentKey,  final ServerWebExchange exchange) {

    	return documentService.getDocument(documentKey)
    			.map(ResponseEntity::ok)
    			.onErrorResume(error -> {
    				if (error instanceof IdClientNotFoundException) {
    					return Mono.just(ResponseEntity.notFound().build());
    				}
    				return Mono.just(ResponseEntity.badRequest().build());
    			});

    }

	@Override
    public  Mono<ResponseEntity<Document>> insertDocument(Mono<Document> document,  final ServerWebExchange exchange) {

		return document.flatMap(request -> documentService.insertDocument(request))
				.map(ResponseEntity::ok)
    			.onErrorResume(error -> {
    				if (error instanceof ItemAlreadyPresent) {
    					return Mono.just(ResponseEntity.status(HttpStatus.FORBIDDEN).build());
    				}
    				return Mono.just(ResponseEntity.badRequest().build());
    			});

    }

	@Override
    public Mono<ResponseEntity<Document>> patchDoc(String documentKey, Mono<Document> document,  final ServerWebExchange exchange) {
    	
		return document.flatMap(request -> documentService.patchDocument(documentKey, request))
				.map(ResponseEntity::ok)
    			.onErrorResume(error -> {
    				if (error instanceof IdClientNotFoundException) {
    					return Mono.just(ResponseEntity.notFound().build());
    				}
    				return Mono.just(ResponseEntity.badRequest().build());
    			});

    }

	@Override
    public Mono<ResponseEntity<Document>> deleteDocument(String documentKey,  final ServerWebExchange exchange) {
		
		return documentService.deleteDocument(documentKey)
				.map(ResponseEntity::ok)
    			.onErrorResume(error -> {
    				if (error instanceof IdClientNotFoundException) {
    					return Mono.just(ResponseEntity.notFound().build());
    				}
    				return Mono.just(ResponseEntity.badRequest().build());
    			});
    	
    }

}
