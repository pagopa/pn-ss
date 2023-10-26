package it.pagopa.pnss.uribuilder.rest;

import it.pagopa.pn.safestorage.generated.openapi.server.v1.api.FileUploadApi;
import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.FileCreationRequest;
import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.FileCreationResponse;
import it.pagopa.pnss.common.utils.LogUtils;
import lombok.CustomLog;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;

import it.pagopa.pnss.common.client.exception.ChecksumException;
import it.pagopa.pnss.uribuilder.service.UriBuilderService;
import reactor.core.publisher.Mono;

@RestController
@CustomLog
public class FileUploadApiController implements FileUploadApi {
	
	@Value("${header.x-checksum-value:#{null}}")
	private String headerXChecksumValue;

    @Value("${queryParam.presignedUrl.traceId}")
    private String xTraceId;

    private final UriBuilderService uriBuilderService;

    public FileUploadApiController(UriBuilderService uriBuilderService) {
        this.uriBuilderService = uriBuilderService;
    }


    @Override
    public Mono<ResponseEntity<FileCreationResponse>> createFile(String xPagopaSafestorageCxId,
																 Mono<FileCreationRequest> fileCreationRequest,
																 final ServerWebExchange exchange) {
		final String CREATE_FILE = "createFile";

		log.info(LogUtils.STARTING_PROCESS_ON, CREATE_FILE, xPagopaSafestorageCxId);

        String xTraceIdValue = exchange.getRequest().getHeaders().getFirst(xTraceId);
        return fileCreationRequest.flatMap(request -> {
        								String checksumValue = null;
										if (headerXChecksumValue != null && !headerXChecksumValue.isBlank()
												&& exchange.getRequest().getHeaders().containsKey(headerXChecksumValue)) {
												checksumValue = exchange.getRequest().getHeaders().getFirst(headerXChecksumValue);
										}
										log.debug(LogUtils.INVOKING_METHOD + LogUtils.ARG + LogUtils.ARG + LogUtils.ARG, "createUriForUploadFile", xPagopaSafestorageCxId, request, checksumValue, xTraceIdValue);
										return uriBuilderService.createUriForUploadFile(xPagopaSafestorageCxId,
        																				request,
        																				checksumValue,
        																				xTraceIdValue);
        						  })
        						  .onErrorResume(ChecksumException.class, throwable -> {
									  log.info(LogUtils.ENDING_PROCESS_WITH_ERROR, CREATE_FILE, throwable, throwable.getMessage());
        							  throw new ResponseStatusException(HttpStatus.BAD_REQUEST,throwable.getMessage());
        						  })
								  .map(fileCreationResponse -> {
									  log.info(LogUtils.ENDING_PROCESS_ON, CREATE_FILE, fileCreationResponse.getKey());
									  return ResponseEntity.ok(fileCreationResponse);
								  });
    }
}
