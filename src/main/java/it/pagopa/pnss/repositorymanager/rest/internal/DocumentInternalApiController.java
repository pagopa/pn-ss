package it.pagopa.pnss.repositorymanager.rest.internal;

import it.pagopa.pn.template.internal.rest.v1.dto.*;
import it.pagopa.pn.template.internal.rest.v1.dto.Error;
import it.pagopa.pnss.common.client.exception.DocumentTypeNotPresentException;
import it.pagopa.pnss.common.client.exception.PatchDocumentExcetpion;
import it.pagopa.pnss.repositorymanager.exception.IllegalDocumentStateException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;

import it.pagopa.pn.template.internal.rest.v1.api.DocumentInternalApi;
import it.pagopa.pnss.common.client.exception.DocumentKeyNotPresentException;
import it.pagopa.pnss.repositorymanager.exception.ItemAlreadyPresent;
import it.pagopa.pnss.repositorymanager.exception.RepositoryManagerException;
import it.pagopa.pnss.repositorymanager.service.DocumentService;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@RestController
@Slf4j
public class DocumentInternalApiController implements DocumentInternalApi {
	
    @Value("${header.x-api-key}")
    private String xApiKey;

    @Value("${header.x-pagopa-safestorage-cx-id}")
    private String xPagopaSafestorageCxId;

	private final DocumentService documentService;

	public DocumentInternalApiController(DocumentService documentService) {
		this.documentService = documentService;
	}

	private DocumentResponse getResponse(Document document) {
		DocumentResponse response = new DocumentResponse();
		response.setDocument(document);
		return response;
	}

	private Mono<ResponseEntity<DocumentResponse>> buildErrorResponse(HttpStatus httpStatus, String errorMsg) {
		DocumentResponse response = new DocumentResponse();
		response.setError(new Error());
		response.getError().setDescription(errorMsg);
		return Mono.just(ResponseEntity.status(httpStatus).body(response));
	}

	private Mono<ResponseEntity<DocumentResponse>> buildErrorResponse(HttpStatus httpStatus, Throwable throwable) {
		DocumentResponse response = new DocumentResponse();
		response.setError(new Error());
		response.getError().setDescription(throwable.getMessage());
		return Mono.just(ResponseEntity.status(httpStatus).body(response));
	}

	private Mono<ResponseEntity<DocumentResponse>> getResponse(String documentKey, Throwable throwable) {
		DocumentResponse response = new DocumentResponse();
		response.setError(new Error());

		response.getError().setDescription(throwable.getMessage());

		if (throwable instanceof ItemAlreadyPresent) {
			log.error("getResponse() : error ItemAlreadyPresent");
			String errorMsg = documentKey == null ? "Document already present"
					: String.format("Document with id %s already present", documentKey);
			return buildErrorResponse(HttpStatus.CONFLICT, errorMsg);
		} else if (throwable instanceof DocumentKeyNotPresentException) {
			log.error("getResponse() : error DocumentKeyNotPresentException");
			String errorMsg = documentKey == null ? "Document not found"
					: String.format("Document with id %s not found", documentKey);
			return buildErrorResponse(HttpStatus.NOT_FOUND, errorMsg);
		} else if (throwable instanceof RepositoryManagerException) {
			log.error("getResponse() : error RepositoryManagerException");
			return buildErrorResponse(HttpStatus.BAD_REQUEST, throwable);
		}else if (throwable instanceof IllegalDocumentStateException) {
			log.error("getResponse() : error IllegalDocumentStateException");
			return buildErrorResponse(HttpStatus.BAD_REQUEST, throwable);
		}else if (throwable instanceof PatchDocumentExcetpion) {
			log.error("getResponse() : error PatchDocumentExcetpion");
			return buildErrorResponse(HttpStatus.BAD_REQUEST, throwable);
		} else if (throwable instanceof DocumentTypeNotPresentException) {
			String errorMsg = "Document type invalide";
			log.error("getResponse() : error DocumentTypeNotPresentException");
			return buildErrorResponse(HttpStatus.BAD_REQUEST, errorMsg);
		} else {
			log.info("getErrorResponse() : other");
			log.error("errore", throwable);
			return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, throwable);
		}
	}

	@Override
	public Mono<ResponseEntity<DocumentResponse>> getDocument(String documentKey, final ServerWebExchange exchange) {

		return documentService.getDocument(documentKey)
				.map(documentOutput -> ResponseEntity.ok(getResponse(documentOutput)))
				.onErrorResume(throwable -> getResponse(documentKey, throwable));

	}

	@Override
	public Mono<ResponseEntity<DocumentResponse>> insertDocument(Mono<DocumentInput> document,
			final ServerWebExchange exchange) {

		return document.flatMap(documentService::insertDocument)
				.map(documentOutput -> ResponseEntity.ok(getResponse(documentOutput)))
				.onErrorResume(throwable -> getResponse(null, throwable));

	}

	@Override
	public Mono<ResponseEntity<DocumentResponse>> patchDoc(String documentKey, Mono<DocumentChanges> documentChanges,
			final ServerWebExchange exchange) {

    	String xPagopaSafestorageCxIdValue = exchange.getRequest().getHeaders().getFirst(xPagopaSafestorageCxId);
    	String xApiKeyValue = exchange.getRequest().getHeaders().getFirst(xApiKey);

        return documentChanges.flatMap(request -> documentService.patchDocument(documentKey, 
        																		request, 
        																		xPagopaSafestorageCxIdValue, 
        																		xApiKeyValue))
                       .map(documentOutput -> ResponseEntity.ok(getResponse(documentOutput)))
                       .onErrorResume(throwable -> getResponse(documentKey, throwable));

	}

	@Override
	public Mono<ResponseEntity<Void>> deleteDocument(String documentKey, final ServerWebExchange exchange) {

		return documentService.deleteDocument(documentKey).map(docType -> ResponseEntity.noContent().<Void>build())
				.onErrorResume(DocumentKeyNotPresentException.class, throwable -> Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND,
						throwable.getMessage(), throwable.getCause())));

	}
}
