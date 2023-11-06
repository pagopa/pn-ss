package it.pagopa.pnss.uribuilder.rest;

import com.amazonaws.services.s3.model.CORSRule;
import it.pagopa.pn.commons.utils.MDCUtils;
import it.pagopa.pn.safestorage.generated.openapi.server.v1.api.FileDownloadApi;
import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.FileDownloadResponse;
import it.pagopa.pnss.common.utils.LogUtils;
import it.pagopa.pnss.uribuilder.service.UriBuilderService;

import lombok.CustomLog;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import static it.pagopa.pnss.common.utils.LogUtils.GET_FILE;
import static it.pagopa.pnss.common.utils.LogUtils.MDC_CORR_ID_KEY;

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

        MDC.clear();
        MDC.put(MDC_CORR_ID_KEY, fileKey);
        log.logStartingProcess(GET_FILE);
        String xTraceIdValue = exchange.getRequest().getHeaders().getFirst(xTraceId);
        return MDCUtils.addMDCToContextAndExecute(uriBuilderService.createUriForDownloadFile(fileKey, xPagopaSafestorageCxId, xTraceIdValue, metadataOnly)
                .map(ResponseEntity::ok)
                .doOnSuccess(result -> log.logEndingProcess(GET_FILE))
                .doOnError(throwable -> log.logEndingProcess(GET_FILE, false, throwable.getMessage())));
    }
}
