package it.pagopa.pnss.uribuilder.rest;

import it.pagopa.pn.commons.utils.MDCUtils;
import it.pagopa.pn.safestorage.generated.openapi.server.v1.api.FileUploadApi;
import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.FileCreationRequest;
import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.FileCreationResponse;
import it.pagopa.pnss.common.exception.IndexingLimitException;
import lombok.CustomLog;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;

import it.pagopa.pnss.common.client.exception.ChecksumException;
import it.pagopa.pnss.uribuilder.service.UriBuilderService;
import reactor.core.publisher.Mono;

import static it.pagopa.pnss.common.utils.LogUtils.CREATE_FILE;
import static it.pagopa.pnss.common.utils.LogUtils.MDC_CORR_ID_KEY;

@RestController
@CustomLog
public class FileUploadApiController implements FileUploadApi {

    @Value("${queryParam.presignedUrl.traceId}")
    private String xTraceId;

    private final UriBuilderService uriBuilderService;

    public FileUploadApiController(UriBuilderService uriBuilderService) {
        this.uriBuilderService = uriBuilderService;
    }


    @Override
    public Mono<ResponseEntity<FileCreationResponse>> createFile(String xPagopaSafestorageCxId, String xChecksumValue, String xChecksum,
																 Mono<FileCreationRequest> fileCreationRequest,
																 final ServerWebExchange exchange) {

        String xTraceIdValue = exchange.getRequest().getHeaders().getFirst(xTraceId);
		MDC.clear();
		MDC.put(MDC_CORR_ID_KEY, xTraceIdValue);
		log.logStartingProcess(CREATE_FILE);

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
