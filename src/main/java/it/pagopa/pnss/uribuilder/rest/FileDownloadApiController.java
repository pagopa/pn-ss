package it.pagopa.pnss.uribuilder.rest;

import it.pagopa.pn.template.rest.v1.api.FileDownloadApi;
import it.pagopa.pn.template.rest.v1.dto.FileDownloadResponse;
import it.pagopa.pnss.common.constant.Constant;
import it.pagopa.pnss.uribuilder.service.UriBuilderService;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@RestController
@Slf4j
public class FileDownloadApiController implements FileDownloadApi {
    @Autowired
    private UriBuilderService uriBuilderService;

    @Value("${queryParam.presignedUrl.traceId}")
    private String xTraceId;

    @Override
    public Mono<ResponseEntity<FileDownloadResponse>> getFile(String fileKey, String xPagopaSafestorageCxId, Boolean metadataOnly,
                                                              final ServerWebExchange exchange) {
        final String GET_FILE = "getFile";

        log.info(Constant.STARTING_PROCESS_ON + Constant.ARG, GET_FILE, fileKey, xPagopaSafestorageCxId);

        String xTraceIdValue = exchange.getRequest().getHeaders().getFirst(xTraceId);
        return  uriBuilderService.createUriForDownloadFile(fileKey, xPagopaSafestorageCxId, xTraceIdValue, metadataOnly).map(ResponseEntity::ok)
                .doOnSuccess(result -> log.info(Constant.ENDING_PROCESS_ON + Constant.ARG, GET_FILE, fileKey,xPagopaSafestorageCxId));


    }
}
