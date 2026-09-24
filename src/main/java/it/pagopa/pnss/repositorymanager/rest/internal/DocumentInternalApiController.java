package it.pagopa.pnss.repositorymanager.rest.internal;

import it.pagopa.pn.commons.utils.MDCUtils;
import it.pagopa.pn.safestorage.generated.openapi.server.v1.api.DocumentInternalApi;
import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.Error;
import it.pagopa.pnss.common.client.exception.DocumentKeyNotPresentException;
import it.pagopa.pnss.common.client.exception.DocumentTypeNotPresentException;
import it.pagopa.pnss.common.client.exception.RetentionException;
import it.pagopa.pnss.common.exception.InvalidNextStatusException;
import it.pagopa.pnss.common.utils.LogUtils;
import it.pagopa.pnss.repositorymanager.exception.IllegalDocumentStateException;
import it.pagopa.pnss.repositorymanager.exception.ItemAlreadyPresent;
import it.pagopa.pnss.repositorymanager.exception.RepositoryManagerException;
import it.pagopa.pnss.repositorymanager.exception.ResourceDeletedException;
import it.pagopa.pnss.configurationproperties.PnSsConfig;
import it.pagopa.pnss.repositorymanager.service.DocumentService;
import lombok.CustomLog;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;

import java.time.DateTimeException;

import static it.pagopa.pnss.common.utils.DynamoDbUtils.DYNAMO_OPTIMISTIC_LOCKING_RETRY;

@RestController
@CustomLog
public class DocumentInternalApiController implements DocumentInternalApi {

    private final String xApiKey;
    private final String xPagopaSafestorageCxId;

	private final DocumentService documentService;

	public DocumentInternalApiController(DocumentService documentService, PnSsConfig pnSsConfig) {
		this.documentService = documentService;
		this.xApiKey = pnSsConfig.getClientInterni().getHeader().getApiKey();
		this.xPagopaSafestorageCxId = pnSsConfig.getClientInterni().getHeader().getPagopaSafestorageCxId();
	}

	private DocumentResponse getResponse(Document document) {
		DocumentResponse response = new DocumentResponse();
		DocumentResponseDocument responseDocument = new DocumentResponseDocument();
		responseDocument.setDocumentKey(document.getDocumentKey());
		responseDocument.setContentType(document.getContentType());
		responseDocument.setDocumentState(document.getDocumentState());
		responseDocument.setDocumentLogicalState(document.getDocumentLogicalState());
		responseDocument.setClientShortCode(document.getClientShortCode());
		responseDocument.setRetentionUntil(document.getRetentionUntil());
		responseDocument.setAvailableUntil(document.getAvailableUntil());
		responseDocument.setCheckSum(document.getCheckSum());
		responseDocument.setContentLenght(document.getContentLenght());
		responseDocument.setDocumentType(document.getDocumentType());
		responseDocument.setTags(document.getTags());
		responseDocument.setLastStatusChangeTimestamp(document.getLastStatusChangeTimestamp());
		response.setDocument(responseDocument);
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
		else if (throwable instanceof ResourceDeletedException.DocumentDeletedException) {
			return buildErrorResponse(HttpStatus.GONE, throwable);
		}
		else {
			log.error("Internal Error ---> {}", throwable.getMessage());
			return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, throwable);
		}
	}

	@Override
	public Mono<ResponseEntity<DocumentResponse>> getDocument(String documentKey, final ServerWebExchange exchange) {
		final String GET_DOCUMENT = "getDocument";
		log.debug("Request header names for '{}' : {}", GET_DOCUMENT, exchange.getRequest().getHeaders().keySet());
		return MDCUtils.addMDCToContextAndExecute(documentService.getDocument(documentKey)
				.map(documentOutput -> ResponseEntity.ok(getResponse(documentOutput)))
				.onErrorResume(throwable -> getResponse(documentKey, throwable)));

	}

	@Override
	public Mono<ResponseEntity<DocumentResponse>> insertDocument(Mono<DocumentInput> document,
			final ServerWebExchange exchange) {
		final String INSERT_DOCUMENT = "insertDocument";
		return document.flatMap(documentInput ->{
					log.debug(LogUtils.INVOKING_METHOD, INSERT_DOCUMENT, documentInput);
					return documentService.insertDocument(documentInput);
				})
				.map(documentOutput -> ResponseEntity.ok(getResponse(documentOutput)))
				.onErrorResume(throwable -> getResponse(null, throwable));

	}

	@Override
	public Mono<ResponseEntity<DocumentResponse>> patchDoc(String documentKey, Mono<DocumentChanges> documentChanges,
			final ServerWebExchange exchange) {
		final String PATCH_DOCUMENT = "patchDoc";
		log.debug("Request header names for '{}' : {}", PATCH_DOCUMENT, exchange.getRequest().getHeaders().keySet());

    	String xPagopaSafestorageCxIdValue = exchange.getRequest().getHeaders().getFirst(xPagopaSafestorageCxId);
    	String xApiKeyValue = exchange.getRequest().getHeaders().getFirst(xApiKey);

        return MDCUtils.addMDCToContextAndExecute(documentChanges.flatMap(request -> documentService.patchDocument(documentKey,
					request,
					xPagopaSafestorageCxIdValue,
					xApiKeyValue).retryWhen(DYNAMO_OPTIMISTIC_LOCKING_RETRY))
                       .map(documentOutput -> ResponseEntity.ok(getResponse(documentOutput)))
                       .onErrorResume(throwable -> getResponse(documentKey, throwable)));

	}

	@Override
	public Mono<ResponseEntity<Void>> deleteDocument(String documentKey, final ServerWebExchange exchange) {
		return documentService.deleteDocument(documentKey).map(docType -> ResponseEntity.noContent().<Void>build())
				.onErrorResume(DocumentKeyNotPresentException.class, throwable -> Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND,
						throwable.getMessage(), throwable.getCause())));

	}
}
