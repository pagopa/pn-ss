package it.pagopa.pnss.repositoryManager.rest.internal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;

import it.pagopa.pn.template.internal.rest.v1.api.DocTypeInternalApi;
import it.pagopa.pn.template.internal.rest.v1.dto.DocumentType;
import it.pagopa.pnss.common.client.exception.IdClientNotFoundException;
import it.pagopa.pnss.repositoryManager.exception.ItemAlreadyPresent;
import it.pagopa.pnss.repositoryManager.service.DocTypesService;
import reactor.core.publisher.Mono;

@RestController
public class DocTypeInternalApiController implements DocTypeInternalApi {
	
	@Autowired
	private DocTypesService docTypesService;

	@Override
    public Mono<ResponseEntity<DocumentType>> getDocType(String typeId,  final ServerWebExchange exchange) {

    	return docTypesService.getDocType(typeId)
    			.map(ResponseEntity::ok)//.map(docTypeDto -> ResponseEntity.ok(docTypeDto))
    			.onErrorResume(error -> {
    				if (error instanceof IdClientNotFoundException) {
    					return Mono.just(ResponseEntity.notFound().build());
    				}
    				return Mono.just(ResponseEntity.badRequest().build());
    			});
    }

	@Override
    public Mono<ResponseEntity<DocumentType>> insertDocType(Mono<DocumentType> documentType,  final ServerWebExchange exchange) {
		
		return documentType.flatMap(request -> docTypesService.insertDocType(request))
				.map(ResponseEntity::ok)
    			.onErrorResume(error -> {
    				if (error instanceof ItemAlreadyPresent) {
    					return Mono.just(ResponseEntity.status(HttpStatus.FORBIDDEN).build());
    				}
    				return Mono.just(ResponseEntity.badRequest().build());
    			});

    }
    
	@Override
    public Mono<ResponseEntity<DocumentType>> updateDocType(String typeId, Mono<DocumentType> documentType,  final ServerWebExchange exchange) {

		return documentType.flatMap(request -> docTypesService.updateDocType(typeId, request))
				.map(ResponseEntity::ok)
    			.onErrorResume(error -> {
    				if (error instanceof IdClientNotFoundException) {
    					return Mono.just(ResponseEntity.notFound().build());
    				}
    				return Mono.just(ResponseEntity.badRequest().build());
    			});
		
    }
	
	@Override
    public Mono<ResponseEntity<DocumentType>> deleteDocType(String typeId,  final ServerWebExchange exchange) {

		return docTypesService.deleteDocType(typeId)
				.map(ResponseEntity::ok)
    			.onErrorResume(error -> {
    				if (error instanceof IdClientNotFoundException) {
    					return Mono.just(ResponseEntity.notFound().build());
    				}
    				return Mono.just(ResponseEntity.badRequest().build());
    			});

    }

}
