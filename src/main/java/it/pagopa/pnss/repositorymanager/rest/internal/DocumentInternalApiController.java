package it.pagopa.pnss.repositorymanager.rest.internal;

import it.pagopa.pn.commons.utils.MDCUtils;
import it.pagopa.pn.safestorage.generated.openapi.server.v1.api.DocumentInternalApi;
import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.Error;
import it.pagopa.pnss.common.client.exception.DocumentTypeNotPresentException;
import it.pagopa.pnss.common.client.exception.RetentionException;
import it.pagopa.pnss.common.exception.InvalidNextStatusException;
import it.pagopa.pnss.common.utils.LogUtils;
import it.pagopa.pnss.repositorymanager.exception.IllegalDocumentStateException;
import lombok.CustomLog;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import it.pagopa.pnss.common.client.exception.DocumentKeyNotPresentException;
import it.pagopa.pnss.repositorymanager.exception.ItemAlreadyPresent;
import it.pagopa.pnss.repositorymanager.exception.RepositoryManagerException;
import it.pagopa.pnss.repositorymanager.service.DocumentService;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import java.time.DateTimeException;

import static it.pagopa.pnss.common.utils.DynamoDbUtils.DYNAMO_OPTIMISTIC_LOCKING_RETRY;

@RestController
@CustomLog
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
		Error error=new Error();
		error.setCode(httpStatus.name());
		response.setError(error);
		response.getError().setDescription(errorMsg);
		return Mono.just(ResponseEntity.status(httpStatus).body(response));
	}

	private Mono<ResponseEntity<DocumentResponse>> buildErrorResponse(HttpStatus httpStatus, Throwable throwable) {
		DocumentResponse response = new DocumentResponse();
		Error error=new Error();
		error.setCode(httpStatus.name());
		response.setError(error);
		response.getError().setDescription(throwable.getMessage());
		return Mono.just(ResponseEntity.status(httpStatus).body(response));
	}

	private Mono<ResponseEntity<DocumentResponse>> getResponse(String documentKey, Throwable throwable) {
		DocumentResponse response = new DocumentResponse();
		response.setError(new Error());

		response.getError().setDescription(throwable.getMessage());

		if (throwable instanceof ItemAlreadyPresent) {
			String errorMsg = documentKey == null ? "Document already present"
					: String.format("Document with id %s already present", documentKey);
			return buildErrorResponse(HttpStatus.CONFLICT, errorMsg);
		} else if (throwable instanceof DocumentKeyNotPresentException) {
			String errorMsg = documentKey == null ? "Document not found"
					: String.format("Document with id %s not found", documentKey);
			return buildErrorResponse(HttpStatus.NOT_FOUND, errorMsg);
		} else if (throwable instanceof RepositoryManagerException) {
			return buildErrorResponse(HttpStatus.BAD_REQUEST, throwable);
		} else if (throwable instanceof IllegalDocumentStateException) {
			return buildErrorResponse(HttpStatus.BAD_REQUEST, throwable);
		} else if (throwable instanceof DocumentTypeNotPresentException) {
			String errorMsg = "Document type not present";
			return buildErrorResponse(HttpStatus.BAD_REQUEST, errorMsg);
		} else if (throwable instanceof InvalidNextStatusException) {
			return buildErrorResponse(HttpStatus.BAD_REQUEST, throwable);
		}
		else if (throwable instanceof NoSuchKeyException) {
			return buildErrorResponse(HttpStatus.BAD_REQUEST, throwable);
		}
		else if (throwable instanceof RetentionException) {
			return buildErrorResponse(HttpStatus.BAD_REQUEST, throwable);
		}
		else if (throwable instanceof DateTimeException) {
			String errorMsg = "Exception in retention date formatting: ";
			return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, errorMsg + throwable.getMessage());
		}
		else {
			log.error("Internal Error ---> {}", throwable.getMessage());
			return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, throwable);
		}
	}

	@Override
	public Mono<ResponseEntity<DocumentResponse>> getDocument(String documentKey, final ServerWebExchange exchange) {
		final String GET_DOCUMENT = "getDocument";
		log.logStartingProcess(GET_DOCUMENT);
		log.debug("Request headers for '{}' : {}", GET_DOCUMENT, exchange.getRequest().getHeaders());
		return MDCUtils.addMDCToContextAndExecute(documentService.getDocument(documentKey)
				.map(documentOutput -> ResponseEntity.ok(getResponse(documentOutput)))
				.doOnSuccess(result -> log.logEndingProcess(GET_DOCUMENT))
				.onErrorResume(throwable -> {
					log.logEndingProcess(GET_DOCUMENT, false, throwable.getMessage());
					return getResponse(documentKey, throwable);
				}));

	}

	@Override
	public Mono<ResponseEntity<DocumentResponse>> insertDocument(Mono<DocumentInput> document,
			final ServerWebExchange exchange) {
		final String INSERT_DOCUMENT = "insertDocument";
		log.logStartingProcess(INSERT_DOCUMENT);
		return document.flatMap(documentInput ->{
					log.debug(LogUtils.INVOKING_METHOD, INSERT_DOCUMENT, documentInput);
					return documentService.insertDocument(documentInput);
				})
				.map(documentOutput -> ResponseEntity.ok(getResponse(documentOutput)))
				.doOnSuccess(result -> log.logEndingProcess(INSERT_DOCUMENT))
				.onErrorResume(throwable -> {
					log.logEndingProcess(INSERT_DOCUMENT, false, throwable.getMessage());
					return getResponse(null, throwable);
				});

	}

	@Override
	public Mono<ResponseEntity<DocumentResponse>> patchDoc(String documentKey, Mono<DocumentChanges> documentChanges,
			final ServerWebExchange exchange) {
		final String PATCH_DOCUMENT = "patchDoc";
		log.logStartingProcess(PATCH_DOCUMENT);
		log.debug("Request headers for '{}' : {}", PATCH_DOCUMENT, exchange.getRequest().getHeaders());

    	String xPagopaSafestorageCxIdValue = exchange.getRequest().getHeaders().getFirst(xPagopaSafestorageCxId);
    	String xApiKeyValue = exchange.getRequest().getHeaders().getFirst(xApiKey);

        return MDCUtils.addMDCToContextAndExecute(documentChanges.flatMap(request -> documentService.patchDocument(documentKey,
					request,
					xPagopaSafestorageCxIdValue,
					xApiKeyValue))
				       .retryWhen(DYNAMO_OPTIMISTIC_LOCKING_RETRY)
                       .map(documentOutput -> ResponseEntity.ok(getResponse(documentOutput)))
				       .doOnSuccess(result->log.logEndingProcess(PATCH_DOCUMENT))
                       .onErrorResume(throwable -> {
						   log.logEndingProcess(PATCH_DOCUMENT, false, throwable.getMessage());
						   return getResponse(documentKey, throwable);
					   }));

	}

	@Override
	public Mono<ResponseEntity<Void>> deleteDocument(String documentKey, final ServerWebExchange exchange) {
		final String DELETE_DOCUMENT = "deleteDocument";

		log.logStartingProcess(DELETE_DOCUMENT);
		return documentService.deleteDocument(documentKey).map(docType -> ResponseEntity.noContent().<Void>build())
				.doOnSuccess(result->log.logEndingProcess(DELETE_DOCUMENT))
				.onErrorResume(DocumentKeyNotPresentException.class, throwable -> {
					log.logEndingProcess(DELETE_DOCUMENT, false, throwable.getMessage());
					return Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND,
							throwable.getMessage(), throwable.getCause()));
				});

	}
}
