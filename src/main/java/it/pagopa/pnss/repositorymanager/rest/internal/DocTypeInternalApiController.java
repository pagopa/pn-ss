package it.pagopa.pnss.repositorymanager.rest.internal;

import it.pagopa.pn.commons.utils.MDCUtils;
import it.pagopa.pn.safestorage.generated.openapi.server.v1.api.DocTypeInternalApi;
import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.DocumentType;
import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.DocumentTypeResponse;
import it.pagopa.pnss.common.utils.LogUtils;
import lombok.CustomLog;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import it.pagopa.pnss.common.client.exception.DocumentTypeNotPresentException;
import it.pagopa.pnss.repositorymanager.exception.ItemAlreadyPresent;
import it.pagopa.pnss.repositorymanager.exception.RepositoryManagerException;
import it.pagopa.pnss.repositorymanager.service.DocTypesService;
import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.Error;
import reactor.core.publisher.Mono;

import static it.pagopa.pnss.common.utils.LogUtils.*;


@RestController
@CustomLog
public class DocTypeInternalApiController implements DocTypeInternalApi {

	private final DocTypesService docTypesService;

	public DocTypeInternalApiController(DocTypesService docTypesService) {
		this.docTypesService = docTypesService;
	}

	private DocumentTypeResponse getResponse(DocumentType docType) {
		DocumentTypeResponse response = new DocumentTypeResponse();
		response.setDocType(docType);
		return response;
	}

	private Mono<ResponseEntity<DocumentTypeResponse>> buildErrorResponse(HttpStatus httpStatus, String errorMsg) {
		DocumentTypeResponse response = new DocumentTypeResponse();
		response.setError(new Error());
		response.getError().setDescription(errorMsg);
		return Mono.just(ResponseEntity.status(httpStatus).body(response));
	}

	private Mono<ResponseEntity<DocumentTypeResponse>> buildErrorResponse(HttpStatus httpStatus, Throwable throwable) {
		DocumentTypeResponse response = new DocumentTypeResponse();
		response.setError(new Error());
		response.getError().setDescription(throwable.getMessage());
		return Mono.just(ResponseEntity.status(httpStatus).body(response));
	}

	private Mono<ResponseEntity<DocumentTypeResponse>> getErrorResponse(String typeId, Throwable throwable) {

		if (throwable instanceof ItemAlreadyPresent) {
			String errorMsg = typeId == null ? "DocType already present"
					: String.format("DocType with id %s already present", typeId);
			return buildErrorResponse(HttpStatus.CONFLICT, errorMsg);
		} else if (throwable instanceof DocumentTypeNotPresentException) {
			String errorMsg = typeId == null ? "Document type not found"
					: String.format("DocumentType with id %s not found", typeId);
			return buildErrorResponse(HttpStatus.NOT_FOUND, errorMsg);
		} else if (throwable instanceof RepositoryManagerException) {
			return buildErrorResponse(HttpStatus.BAD_REQUEST, throwable);
		} else {
			log.error("getErrorResponse() : other error {}", throwable.getMessage());
			return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, throwable);
		}
	}

	@Override
	public Mono<ResponseEntity<DocumentTypeResponse>> getDocType(String typeId, final ServerWebExchange exchange) {
		log.logStartingProcess(GET_DOC_TYPE);
		return docTypesService.getDocType(typeId).map(docType -> ResponseEntity.ok(getResponse(docType)))
				.onErrorResume(throwable -> getErrorResponse(typeId, throwable))
				.doOnError(throwable -> log.logEndingProcess(GET_DOC_TYPE, false, throwable.getMessage()))
				.doOnSuccess(result -> log.logEndingProcess(GET_DOC_TYPE));
	}

	@Override
	public Mono<ResponseEntity<DocumentTypeResponse>> insertDocType(Mono<DocumentType> documentType, final ServerWebExchange exchange) {
		log.logStartingProcess(INSERT_DOC_TYPE);
		return documentType.flatMap(docTypesService::insertDocType)
				.map(docType -> ResponseEntity.ok(getResponse(docType)))
				.onErrorResume(throwable -> getErrorResponse(null, throwable))
				.doOnError(throwable -> log.logEndingProcess(INSERT_DOC_TYPE, false, throwable.getMessage()))
				.doOnSuccess(result -> log.logEndingProcess(INSERT_DOC_TYPE));

	}

	@Override
	public Mono<ResponseEntity<DocumentTypeResponse>> updateDocType(String typeId, Mono<DocumentType> documentType, final ServerWebExchange exchange) {
		log.logStartingProcess(UPDATE_DOC_TYPE);
		return documentType.flatMap(request -> docTypesService.updateDocType(typeId, request))
				.map(docType -> ResponseEntity.ok(getResponse(docType)))
				.onErrorResume(throwable -> getErrorResponse(typeId, throwable))
				.doOnError(throwable -> log.logEndingProcess(UPDATE_DOC_TYPE, false, throwable.getMessage()))
				.doOnSuccess(result -> log.logEndingProcess(UPDATE_DOC_TYPE));
	}

	@Override
	public Mono<ResponseEntity<Void>> deleteDocType(String typeId, final ServerWebExchange exchange) {
		log.logStartingProcess(DELETE_DOC_TYPE);
		return docTypesService.deleteDocType(typeId).map(docType -> ResponseEntity.noContent().<Void>build())
				.onErrorResume(DocumentTypeNotPresentException.class, throwable -> Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, throwable.getMessage(), throwable.getCause())))
                .doOnError(throwable -> log.logEndingProcess(DELETE_DOC_TYPE, false, throwable.getMessage()))
				.doOnSuccess(result -> log.logEndingProcess(DELETE_DOC_TYPE));
	}
}
