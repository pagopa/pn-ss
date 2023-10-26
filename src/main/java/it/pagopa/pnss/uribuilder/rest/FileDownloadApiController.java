package it.pagopa.pnss.uribuilder.rest;

import it.pagopa.pn.safestorage.generated.openapi.server.v1.api.FileDownloadApi;
import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.FileDownloadResponse;
import it.pagopa.pnss.common.utils.LogUtils;
import it.pagopa.pnss.uribuilder.service.UriBuilderService;

import lombok.CustomLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import static it.pagopa.pnss.common.utils.LogUtils.GET_FILE;

@RestController
@CustomLog
public class FileDownloadApiController implements FileDownloadApi {
    @Autowired
    private UriBuilderService uriBuilderService;

    @Value("${queryParam.presignedUrl.traceId}")
    private String xTraceId;

    @Override
    public Mono<ResponseEntity<FileDownloadResponse>> getFile(String fileKey, String xPagopaSafestorageCxId, Boolean metadataOnly,
                                                              final ServerWebExchange exchange) {

        log.logStartingProcess(GET_FILE);
        String xTraceIdValue = exchange.getRequest().getHeaders().getFirst(xTraceId);
        return uriBuilderService.createUriForDownloadFile(fileKey, xPagopaSafestorageCxId, xTraceIdValue, metadataOnly)
                .map(ResponseEntity::ok)
                .doOnSuccess(result->log.info(LogUtils.ENDING_PROCESS, GET_FILE));
    }
}
