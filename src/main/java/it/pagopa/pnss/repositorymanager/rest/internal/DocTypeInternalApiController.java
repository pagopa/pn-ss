package it.pagopa.pnss.repositorymanager.rest.internal;

import it.pagopa.pnss.common.constant.Constant;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;

import it.pagopa.pn.template.internal.rest.v1.api.DocTypeInternalApi;
import it.pagopa.pn.template.internal.rest.v1.dto.DocumentType;
import it.pagopa.pn.template.internal.rest.v1.dto.DocumentTypeResponse;
import it.pagopa.pn.template.internal.rest.v1.dto.Error;
import it.pagopa.pnss.common.client.exception.DocumentKeyNotPresentException;
import it.pagopa.pnss.common.client.exception.DocumentTypeNotPresentException;
import it.pagopa.pnss.repositorymanager.exception.ItemAlreadyPresent;
import it.pagopa.pnss.repositorymanager.exception.RepositoryManagerException;
import it.pagopa.pnss.repositorymanager.service.DocTypesService;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;


@RestController
@Slf4j
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
		final String GET_DOC_TYPE = "getDocType";

		log.info(Constant.STARTING_PROCESS_ON, GET_DOC_TYPE, typeId);

		return docTypesService.getDocType(typeId).map(docType -> {
					log.info(Constant.ENDING_PROCESS_ON, GET_DOC_TYPE, typeId);
					return ResponseEntity.ok(getResponse(docType));
				})
				.onErrorResume(throwable -> {
					log.info(Constant.ENDING_PROCESS_WITH_ERROR, GET_DOC_TYPE, throwable, throwable.getMessage());
					return getErrorResponse(typeId, throwable);
				});
	}

	@Override
	public Mono<ResponseEntity<DocumentTypeResponse>> insertDocType(Mono<DocumentType> documentType,
			final ServerWebExchange exchange) {
		final String INSERT_DOC_TYPE = "insertDocType";

		return documentType.doOnNext(docType ->{
				log.info(Constant.STARTING_PROCESS_ON, INSERT_DOC_TYPE, docType == null ? null : docType.getTipoDocumento());
				})
				.flatMap(docType -> {
					log.debug(Constant.INVOKING_METHOD, INSERT_DOC_TYPE, docType);
					return docTypesService.insertDocType(docType);
				})
				.map(docType -> {
					log.info(Constant.ENDING_PROCESS_ON, INSERT_DOC_TYPE, docType.getTipoDocumento());
					return ResponseEntity.ok(getResponse(docType));
				})
				.onErrorResume(throwable -> {
					log.info(Constant.ENDING_PROCESS_WITH_ERROR, INSERT_DOC_TYPE, throwable, throwable.getMessage());
					return getErrorResponse(null, throwable);
				});

	}

	@Override
	public Mono<ResponseEntity<DocumentTypeResponse>> updateDocType(String typeId, Mono<DocumentType> documentType,
			final ServerWebExchange exchange) {
		final String UPDATE_DOC_TYPE = "updateDocType";

		log.info(Constant.STARTING_PROCESS_ON, UPDATE_DOC_TYPE, typeId);

		return documentType.flatMap(request -> {
					log.debug(Constant.INVOKING_METHOD + " - '{}'", UPDATE_DOC_TYPE, typeId, request);
					return docTypesService.updateDocType(typeId, request);
				})
				.map(docType -> {
					log.info(Constant.ENDING_PROCESS_ON, UPDATE_DOC_TYPE, docType);
					return ResponseEntity.ok(getResponse(docType));
				})
				.onErrorResume(throwable -> {
					log.info(Constant.ENDING_PROCESS_WITH_ERROR, UPDATE_DOC_TYPE, throwable, throwable.getMessage());
					return getErrorResponse(typeId, throwable);
				});

	}

	@Override
	public Mono<ResponseEntity<Void>> deleteDocType(String typeId, final ServerWebExchange exchange) {
		final String DELETE_DOC_TYPE = "deleteDocType";

		log.info(Constant.STARTING_PROCESS_ON, DELETE_DOC_TYPE, typeId);

		log.debug(Constant.INVOKING_METHOD, DELETE_DOC_TYPE, typeId);
		return docTypesService.deleteDocType(typeId).map(docType -> {
					log.info(Constant.ENDING_PROCESS_ON, DELETE_DOC_TYPE, docType);
			return ResponseEntity.noContent().<Void>build();
			})
				.onErrorResume(DocumentTypeNotPresentException.class,
						throwable -> {
							log.info(Constant.ENDING_PROCESS_WITH_ERROR, DELETE_DOC_TYPE, throwable, throwable.getMessage());
						return Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND,
									throwable.getMessage(), throwable.getCause()));
						});

	}
}
