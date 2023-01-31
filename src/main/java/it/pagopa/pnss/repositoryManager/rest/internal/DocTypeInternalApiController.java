package it.pagopa.pnss.repositoryManager.rest.internal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;

import it.pagopa.pn.template.internal.rest.v1.api.DocTypeInternalApi;
import it.pagopa.pn.template.internal.rest.v1.dto.DocumentType;
import it.pagopa.pnss.repositoryManager.service.DocTypesService;
import reactor.core.publisher.Mono;

@RestController
public class DocTypeInternalApiController implements DocTypeInternalApi {
	
	@Autowired
	private DocTypesService docTypesService;

	@Override
    public Mono<ResponseEntity<DocumentType>> getDocType(String typeId,  final ServerWebExchange exchange) {

    	return docTypesService.getDocType(typeId).map(x -> new ResponseEntity<>(x, HttpStatus.OK));

    }

	@Override
    public Mono<ResponseEntity<DocumentType>> insertDocType(Mono<DocumentType> documentType,  final ServerWebExchange exchange) {
		
		return documentType.flatMap(request -> docTypesService.insertDocType(request))
							.map(x -> new ResponseEntity<>(x, HttpStatus.OK));

    }
    
	@Override
    public Mono<ResponseEntity<DocumentType>> updateDocType(String typeId, Mono<DocumentType> documentType,  final ServerWebExchange exchange) {

		return documentType.flatMap(request -> docTypesService.updateDocType(typeId, request))
				.map(x -> new ResponseEntity<>(x, HttpStatus.OK));

    }
	
	@Override
    public Mono<ResponseEntity<DocumentType>> deleteDocType(String typeId,  final ServerWebExchange exchange) {

		return docTypesService.deleteDocType(typeId).map(x -> new ResponseEntity<>(x, HttpStatus.OK));

    }

}
