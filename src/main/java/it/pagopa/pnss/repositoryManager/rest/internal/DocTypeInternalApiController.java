package it.pagopa.pnss.repositoryManager.rest.internal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;

import it.pagopa.pn.template.internal.rest.v1.api.DocTypeInternalApi;
import it.pagopa.pn.template.internal.rest.v1.dto.DocumentType;
import it.pagopa.pn.template.internal.rest.v1.dto.DocumentTypeResponse;
import it.pagopa.pn.template.internal.rest.v1.dto.Error;
import it.pagopa.pnss.common.client.exception.IdClientNotFoundException;
import it.pagopa.pnss.repositoryManager.exception.ItemAlreadyPresent;
import it.pagopa.pnss.repositoryManager.service.DocTypesService;
import reactor.core.publisher.Mono;

@RestController
public class DocTypeInternalApiController implements DocTypeInternalApi {
	
	@Autowired
	private DocTypesService docTypesService;
	
	private DocumentTypeResponse getResponse(DocumentType docType) {
		DocumentTypeResponse response = new DocumentTypeResponse();
		response.setDocType(docType);
		return response;
	}
	
	private  Mono<ResponseEntity<DocumentTypeResponse>> getResponse(String typeId, Throwable throwable) 
	{
		DocumentTypeResponse response = new DocumentTypeResponse();
		response.setError(new Error());

		if (throwable instanceof ItemAlreadyPresent) {
			response.getError().setDescription(typeId == null ? "DocType already present" : String.format("DocType with id %s already present", typeId));
			return Mono.just(ResponseEntity.status(HttpStatus.FORBIDDEN).body(response));
		}
		else if (throwable instanceof IdClientNotFoundException) {
			response.getError().setDescription(typeId == null ? "DocType not found" : String.format("DocType with id %s not found", typeId));
			return Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND).body(response));
		}
		response.getError().setDescription(throwable.getMessage());
		return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response));
	}

	@Override
    public Mono<ResponseEntity<DocumentTypeResponse>> getDocType(String typeId,  final ServerWebExchange exchange) {
		
		return docTypesService.getDocType(typeId)
			.map(docType -> ResponseEntity.ok(getResponse(docType)))
			.onErrorResume(throwable -> getResponse(typeId, throwable));
    }

	@Override
    public Mono<ResponseEntity<DocumentTypeResponse>> insertDocType(Mono<DocumentType> documentType,  final ServerWebExchange exchange) {
		
		return documentType.flatMap(request -> docTypesService.insertDocType(request))
				.map(docType -> ResponseEntity.ok(getResponse(docType)))
				.onErrorResume(throwable -> getResponse(null, throwable));

    }
    
	@Override
    public Mono<ResponseEntity<DocumentTypeResponse>> updateDocType(String typeId, Mono<DocumentType> documentType,  final ServerWebExchange exchange) {

		return documentType.flatMap(request -> docTypesService.updateDocType(typeId, request))
				.map(docType -> ResponseEntity.ok(getResponse(docType)))
				.onErrorResume(throwable -> getResponse(typeId, throwable));
		
    }
	
	@Override
    public Mono<ResponseEntity<DocumentTypeResponse>> deleteDocType(String typeId,  final ServerWebExchange exchange) {

		return docTypesService.deleteDocType(typeId)
				.map(docType -> ResponseEntity.ok(getResponse(docType)))
				.onErrorResume(throwable -> getResponse(typeId, throwable));

    }

}
