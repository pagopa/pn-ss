package it.pagopa.pnss.repositoryManager.rest.internal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;

import it.pagopa.pn.template.internal.rest.v1.api.DocumentInternalApi;
import it.pagopa.pn.template.internal.rest.v1.dto.Document;
import it.pagopa.pn.template.internal.rest.v1.dto.DocumentResponse;
import it.pagopa.pn.template.internal.rest.v1.dto.Error;
import it.pagopa.pnss.common.client.exception.IdClientNotFoundException;
import it.pagopa.pnss.repositoryManager.exception.ItemAlreadyPresent;
import it.pagopa.pnss.repositoryManager.exception.RepositoryManagerException;
import it.pagopa.pnss.repositoryManager.service.DocumentService;
import reactor.core.publisher.Mono;

@RestController
public class DocumentInternalApiController implements DocumentInternalApi {

	@Autowired
	private DocumentService documentService;
	
	private DocumentResponse getResponse(Document document) {
		DocumentResponse response = new DocumentResponse();
		response.setDocument(document);
		return response;
	}
	
	private  Mono<ResponseEntity<DocumentResponse>> getResponse(String documentKey, Throwable throwable) 
	{
		DocumentResponse response = new DocumentResponse();
		response.setError(new Error());

		if (throwable instanceof ItemAlreadyPresent) {
			response.getError().setDescription(documentKey == null ? "Document already present" : String.format("DocType with id %s already present", documentKey));
			return Mono.just(ResponseEntity.status(HttpStatus.FORBIDDEN).body(response));
		}
		else if (throwable instanceof IdClientNotFoundException) {
			response.getError().setDescription(documentKey == null ? "Document not found" : String.format("DocType with id %s not found", documentKey));
			return Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND).body(response));
		}
		else if (throwable instanceof RepositoryManagerException) {
			response.getError().setDescription("Document has incorrect attribute" );
			return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response));			
		}
		response.getError().setDescription(throwable.getMessage());
		return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response));
	}
	
	@Override
    public Mono<ResponseEntity<DocumentResponse>> getDocument(String documentKey,  final ServerWebExchange exchange) {

		return documentService.getDocument(documentKey)
				.map(documentOutput -> ResponseEntity.ok(getResponse(documentOutput)))
				.onErrorResume(throwable -> getResponse(documentKey, throwable));

    }

	@Override
    public  Mono<ResponseEntity<DocumentResponse>> insertDocument(Mono<Document> document,  final ServerWebExchange exchange) {

		return document.flatMap(request -> documentService.insertDocument(request))
				.map(documentOutput -> ResponseEntity.ok(getResponse(documentOutput)))
				.onErrorResume(throwable -> getResponse(null, throwable));

    }

	@Override
    public Mono<ResponseEntity<DocumentResponse>> patchDoc(String documentKey, Mono<Document> document,  final ServerWebExchange exchange) {
    	
		return document.flatMap(request -> documentService.patchDocument(documentKey, request))
				.map(documentOutput -> ResponseEntity.ok(getResponse(documentOutput)))
				.onErrorResume(throwable -> getResponse(documentKey, throwable));

    }

	@Override
    public Mono<ResponseEntity<DocumentResponse>> deleteDocument(String documentKey,  final ServerWebExchange exchange) {
		
		return documentService.deleteDocument(documentKey)
				.map(documentOutput -> ResponseEntity.ok(getResponse(documentOutput)))
				.onErrorResume(throwable -> getResponse(documentKey, throwable));
    	
    }

}
