package it.pagopa.pnss.repositorymanager.rest.internal;

import it.pagopa.pn.template.internal.rest.v1.dto.*;
import it.pagopa.pn.template.internal.rest.v1.dto.Error;
import it.pagopa.pnss.common.client.exception.DocumentTypeNotPresentException;
import it.pagopa.pnss.common.client.exception.PatchDocumentExcetpion;
import it.pagopa.pnss.common.client.exception.RetentionException;
import it.pagopa.pnss.common.constant.Constant;
import it.pagopa.pnss.common.exception.InvalidNextStatusException;
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
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;

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
			String errorMsg = documentKey == null ? "Document already present"
					: String.format("Document with id %s already present", documentKey);
			return buildErrorResponse(HttpStatus.CONFLICT, errorMsg);
		} else if (throwable instanceof DocumentKeyNotPresentException) {
			String errorMsg = documentKey == null ? "Document not found"
					: String.format("Document with id %s not found", documentKey);
			return buildErrorResponse(HttpStatus.NOT_FOUND, errorMsg);
		} else if (throwable instanceof RepositoryManagerException) {
			return buildErrorResponse(HttpStatus.BAD_REQUEST, throwable);
		}else if (throwable instanceof IllegalDocumentStateException) {
			return buildErrorResponse(HttpStatus.BAD_REQUEST, throwable);
		} else if (throwable instanceof DocumentTypeNotPresentException) {
			String errorMsg = "Document type not present";
			return buildErrorResponse(HttpStatus.BAD_REQUEST, errorMsg);
		} else if (throwable instanceof InvalidNextStatusException) {
			return buildErrorResponse(HttpStatus.BAD_REQUEST, throwable);
		}
		else if (throwable instanceof RetentionException) {
			return buildErrorResponse(HttpStatus.NOT_FOUND, throwable);
		}
		else {
			log.error("Internal Error ---> {}", throwable.getMessage());
			return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, throwable);
		}
	}

	@Override
	public Mono<ResponseEntity<DocumentResponse>> getDocument(String documentKey, final ServerWebExchange exchange) {
		final String GET_DOCUMENT = "getDocument";

		log.info(Constant.STARTING_PROCESS_ON, GET_DOCUMENT, documentKey);
		log.debug(Constant.INVOKING_METHOD, GET_DOCUMENT, documentKey);
		return documentService.getDocument(documentKey)
				.map(documentOutput -> {
					log.info(Constant.ENDING_PROCESS_ON, GET_DOCUMENT, documentKey);
					return ResponseEntity.ok(getResponse(documentOutput));
				})
				.onErrorResume(throwable -> {
					log.info(Constant.ENDING_PROCESS_WITH_ERROR, GET_DOCUMENT, throwable, throwable.getMessage());
					return getResponse(documentKey, throwable);
				});

	}

	@Override
	public Mono<ResponseEntity<DocumentResponse>> insertDocument(Mono<DocumentInput> document,
			final ServerWebExchange exchange) {
		final String INSERT_DOCUMENT = "insertDocument";

		return document.doOnNext(doc ->log.info(Constant.STARTING_PROCESS_ON, INSERT_DOCUMENT, doc == null ? null : doc.getDocumentKey()))
				.flatMap(documentInput ->{
					log.debug(Constant.INVOKING_METHOD, INSERT_DOCUMENT, documentInput);
					return documentService.insertDocument(documentInput);
				})
				.map(documentOutput -> {
					log.info(Constant.ENDING_PROCESS_ON, INSERT_DOCUMENT, documentOutput.getDocumentKey());
					return ResponseEntity.ok(getResponse(documentOutput));
				})
				.onErrorResume(throwable -> {
					log.info(Constant.ENDING_PROCESS_WITH_ERROR, INSERT_DOCUMENT, throwable, throwable.getMessage());
					return getResponse(null, throwable);
				});

	}

	@Override
	public Mono<ResponseEntity<DocumentResponse>> patchDoc(String documentKey, Mono<DocumentChanges> documentChanges,
			final ServerWebExchange exchange) {
		final String PATCH_DOCUMENT = "patchDoc";

		log.info(Constant.STARTING_PROCESS_ON, PATCH_DOCUMENT, documentKey);

    	String xPagopaSafestorageCxIdValue = exchange.getRequest().getHeaders().getFirst(xPagopaSafestorageCxId);
    	String xApiKeyValue = exchange.getRequest().getHeaders().getFirst(xApiKey);

        return documentChanges.flatMap(request -> {
					log.debug(Constant.INVOKING_METHOD + " - '{}' - '{}' - '{}'", PATCH_DOCUMENT, documentKey, request, xPagopaSafestorageCxIdValue, xApiKeyValue);
				return documentService.patchDocument(documentKey,
							request,
							xPagopaSafestorageCxIdValue,
							xApiKeyValue);
				})
                       .map(documentOutput -> {
						   log.info(Constant.ENDING_PROCESS_ON, PATCH_DOCUMENT, documentOutput.getDocumentKey());
						   return ResponseEntity.ok(getResponse(documentOutput));
					   })
                       .onErrorResume(throwable -> {
						   log.info(Constant.ENDING_PROCESS_WITH_ERROR, PATCH_DOCUMENT, throwable, throwable.getMessage());
						   return getResponse(documentKey, throwable);
					   });

	}

	@Override
	public Mono<ResponseEntity<Void>> deleteDocument(String documentKey, final ServerWebExchange exchange) {
		final String DELETE_DOCUMENT = "deleteDocument";

		log.info(Constant.STARTING_PROCESS_ON, DELETE_DOCUMENT, documentKey);

		log.debug(Constant.INVOKING_METHOD, DELETE_DOCUMENT, documentKey);
		return documentService.deleteDocument(documentKey).map(docType -> {
					log.info(Constant.ENDING_PROCESS_ON, DELETE_DOCUMENT, docType);
			return ResponseEntity.noContent().<Void>build();
				})
				.onErrorResume(DocumentKeyNotPresentException.class, throwable -> {
					log.info(Constant.ENDING_PROCESS_WITH_ERROR, DELETE_DOCUMENT, throwable, throwable.getMessage());
					return Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND,
							throwable.getMessage(), throwable.getCause()));
				});

	}
}
