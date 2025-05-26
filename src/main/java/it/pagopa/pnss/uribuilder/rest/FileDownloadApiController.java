package it.pagopa.pnss.uribuilder.rest;

import it.pagopa.pn.commons.utils.MDCUtils;
import it.pagopa.pn.safestorage.generated.openapi.server.v1.api.FileDownloadApi;
import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.FileDownloadResponse;
import it.pagopa.pnss.uribuilder.service.UriBuilderService;

import lombok.CustomLog;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.UUID;

import static it.pagopa.pnss.common.utils.LogUtils.GET_FILE;
import static it.pagopa.pnss.common.utils.LogUtils.MDC_CORR_ID_KEY;

@RestController
@CustomLog
public class FileDownloadApiController implements FileDownloadApi {
    private final UriBuilderService uriBuilderService;
    private final Environment env;

    @Value("${queryParam.presignedUrl.traceId}")
    private String xTraceId;

    public FileDownloadApiController(UriBuilderService uriBuilderService, Environment env) {
        this.uriBuilderService = uriBuilderService;
        this.env = env;
    }

    @Override
    public Mono<ResponseEntity<FileDownloadResponse>> getFile(String fileKey, String xPagopaSafestorageCxId, Boolean metadataOnly, Boolean tags,
                                                              final ServerWebExchange exchange) {

        MDC.clear();
        MDC.put(MDC_CORR_ID_KEY, fileKey);
        String xTraceIdValue = exchange.getRequest().getQueryParams().getFirst(xTraceId);
        xTraceIdValue = (xTraceIdValue == null) ? UUID.randomUUID().toString() : xTraceIdValue;
        log.logStartingProcess(GET_FILE);
        return MDCUtils.addMDCToContextAndExecute(uriBuilderService.createUriForDownloadFile(fileKey, xPagopaSafestorageCxId, xTraceIdValue, metadataOnly, tags)
                .map(ResponseEntity::ok)
                .doOnSuccess(result -> log.logEndingProcess(GET_FILE))
                .doOnError(throwable -> log.logEndingProcess(GET_FILE, false, throwable.getMessage())));
    }
}
