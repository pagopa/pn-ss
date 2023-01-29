package it.pagopa.pnss.repositorymanager.rest.internal;

import static org.springframework.http.HttpStatus.OK;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;

import it.pagopa.pn.template.internal.rest.v1.api.DocTypeInternalApi;
import it.pagopa.pn.template.internal.rest.v1.dto.DocumentType;
import it.pagopa.pnss.repositorymanager.service.DocTypesService;
import reactor.core.publisher.Mono;

@RestController
public class DocTypeInternalApiController implements DocTypeInternalApi {
	
	@Autowired
	private DocTypesService docTypesService;

	@Override
    public Mono<ResponseEntity<DocumentType>> getDocType(String typeId,  final ServerWebExchange exchange) {

    	DocumentType documentType = docTypesService.getDocType(typeId);
    	return Mono.just(ResponseEntity.ok().body(documentType));

    }

	@Override
    public Mono<ResponseEntity<DocumentType>> insertDocType(Mono<DocumentType> documentType,  final ServerWebExchange exchange) {

    	return documentType.map(request -> {
    		DocumentType documentTypeInserted = docTypesService.insertDocType(request);
    		return ResponseEntity.ok().body(documentTypeInserted);
    	});

    }
    
	@Override
    public Mono<ResponseEntity<DocumentType>> updateDocType(String typeId, Mono<DocumentType> documentType,  final ServerWebExchange exchange) {

    	return documentType.map(request -> {
    		DocumentType documentTypeInserted = docTypesService.updateDocType(typeId, request);
    		return ResponseEntity.ok().body(documentTypeInserted);
    	});

    }
	
	@Override
    public Mono<ResponseEntity<Void>> deleteDocType(String typeId,  final ServerWebExchange exchange) {

    	docTypesService.deleteDocType(typeId);
    	return Mono.just(new ResponseEntity<>(OK));	

    }

}
