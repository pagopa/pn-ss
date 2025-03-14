package it.pagopa.pnss.uribuilder.rest;

import it.pagopa.pn.commons.utils.MDCUtils;
import it.pagopa.pn.safestorage.generated.openapi.server.v1.api.FileUploadApi;
import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.FileCreationRequest;
import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.FileCreationResponse;
import it.pagopa.pnss.common.client.exception.DocumentKeyNotPresentException;
import it.pagopa.pnss.common.exception.IndexingLimitException;
import it.pagopa.pnss.common.exception.PutTagsBadRequestException;
import lombok.CustomLog;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;

import it.pagopa.pnss.common.client.exception.ChecksumException;
import it.pagopa.pnss.uribuilder.service.UriBuilderService;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.UUID;

import static it.pagopa.pnss.common.utils.LogUtils.CREATE_FILE;
import static it.pagopa.pnss.common.utils.LogUtils.MDC_CORR_ID_KEY;

@RestController
@CustomLog
public class FileUploadApiController implements FileUploadApi {

    @Value("${queryParam.presignedUrl.traceId}")
    private String xTraceId;

    private final UriBuilderService uriBuilderService;
    private final Environment env;

    public FileUploadApiController(UriBuilderService uriBuilderService, Environment env) {
        this.uriBuilderService = uriBuilderService;
        this.env = env;
    }

    @ExceptionHandler(PutTagsBadRequestException.class)
    public ResponseEntity<String> handlePutTagsBadRequestException(PutTagsBadRequestException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
    }

    @ExceptionHandler(DocumentKeyNotPresentException.class)
    public ResponseEntity<String> handleDocumentKeyNotPresentException(DocumentKeyNotPresentException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
    }

    @Override
    public Mono<ResponseEntity<FileCreationResponse>> createFile(String xPagopaSafestorageCxId, String xChecksumValue, String xChecksum,
																 Mono<FileCreationRequest> fileCreationRequest,
																 final ServerWebExchange exchange) {
        MDC.clear();
		MDC.put(MDC_CORR_ID_KEY, xTraceIdValue);
		log.logStartingProcess(CREATE_FILE);
        // Nelle run di localdev, viene settato un traceId randomico
        boolean isLocal = Arrays.asList(env.getActiveProfiles()).contains("local");
        String xTraceIdValue = !isLocal ? exchange.getRequest().getHeaders().getFirst(xTraceId) : UUID.randomUUID().toString();
        return MDCUtils.addMDCToContextAndExecute(fileCreationRequest.flatMap(request -> uriBuilderService.createUriForUploadFile(xPagopaSafestorageCxId,
                                                        request,
                                                        xChecksumValue,
                                                        xTraceIdValue))
        						  .onErrorResume(ChecksumException.class, throwable -> Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST,throwable.getMessage())))
        						  .onErrorResume(IndexingLimitException.class, throwable -> Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST,throwable.getMessage())))
								  .map(ResponseEntity::ok)
				                  .doOnError(throwable -> log.logEndingProcess(CREATE_FILE, false, throwable.getMessage()))
				                  .doOnSuccess(result->log.logEndingProcess(CREATE_FILE)));
    }
}
