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

        log.info(Constant.STARTING_PROCESS_ON, GET_FILE, fileKey);

        String xTraceIdValue = exchange.getRequest().getHeaders().getFirst(xTraceId);
        log.debug(Constant.INVOKING_METHOD + Constant.ARG + Constant.ARG + Constant.ARG, "createUriForDownloadFile", fileKey, xPagopaSafestorageCxId, xTraceIdValue, metadataOnly);
        Mono<ResponseEntity<FileDownloadResponse>> fileDownloadResponseMono = uriBuilderService.createUriForDownloadFile(fileKey, xPagopaSafestorageCxId, xTraceIdValue, metadataOnly).map(ResponseEntity::ok);
        log.info(Constant.ENDING_PROCESS, GET_FILE);
        return fileDownloadResponseMono;
    }
}
