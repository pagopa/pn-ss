package it.pagopa.pnss.uribuilder.rest;

import it.pagopa.pn.commons.utils.MDCUtils;
import it.pagopa.pn.safestorage.generated.openapi.server.v1.api.FileMetadataUpdateApi;
import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.OperationResultCodeResponse;
import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.UpdateFileMetadataRequest;
import it.pagopa.pnss.uribuilder.service.FileMetadataUpdateService;
import lombok.CustomLog;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import static it.pagopa.pnss.common.utils.LogUtils.MDC_CORR_ID_KEY;
import static it.pagopa.pnss.common.utils.LogUtils.UPDATE_FILE_METADATA;

@RestController
@CustomLog
public class FileMetadataUpdateApiController implements FileMetadataUpdateApi {

    final FileMetadataUpdateService fileMetadataUpdateService;

    @Value("${header.x-api-key}")
    private String apiKey;

    @Value("${header.x-pagopa-safestorage-cx-id}")
    private String pagopaSafestorageCxId;

    public FileMetadataUpdateApiController(FileMetadataUpdateService fileMetadataUpdateService) {
        this.fileMetadataUpdateService = fileMetadataUpdateService;
    }

    @Override
    public Mono<ResponseEntity<OperationResultCodeResponse>> updateFileMetadata(String fileKey, String xPagopaSafestorageCxId,
                                                                                Mono<UpdateFileMetadataRequest> updateFileMetadataRequest
            , final ServerWebExchange exchange) {

        MDC.clear();
        MDC.put(MDC_CORR_ID_KEY, fileKey);
        log.logStartingProcess(UPDATE_FILE_METADATA);

        String pagopaSafestorageCxIdValue = exchange.getRequest().getHeaders().getFirst(pagopaSafestorageCxId);
        String apiKeyValue = exchange.getRequest().getHeaders().getFirst(apiKey);

        return MDCUtils.addMDCToContextAndExecute(updateFileMetadataRequest.flatMap(request -> fileMetadataUpdateService.updateMetadata(fileKey,
                xPagopaSafestorageCxId,
                request,
                pagopaSafestorageCxIdValue,
                apiKeyValue)).map(ResponseEntity::ok))
                .doOnError(throwable -> log.logEndingProcess(UPDATE_FILE_METADATA, false, throwable.getMessage()))
                .doOnSuccess(result->log.logEndingProcess(UPDATE_FILE_METADATA));
    }
}
