package it.pagopa.pnss.uribuilder.rest;

import it.pagopa.pn.template.rest.v1.api.FileMetadataUpdateApi;
import it.pagopa.pn.template.rest.v1.dto.OperationResultCodeResponse;
import it.pagopa.pn.template.rest.v1.dto.UpdateFileMetadataRequest;
import it.pagopa.pnss.common.constant.Constant;
import it.pagopa.pnss.uribuilder.service.FileMetadataUpdateService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@RestController
@Slf4j
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
        final String UPDATE_FILE_METADATA = "updateFileMetadata";

        log.info(Constant.STARTING_PROCESS_ON, UPDATE_FILE_METADATA, fileKey);

        String pagopaSafestorageCxIdValue = exchange.getRequest().getHeaders().getFirst(pagopaSafestorageCxId);
        String apiKeyValue = exchange.getRequest().getHeaders().getFirst(apiKey);

        Mono<ResponseEntity<OperationResultCodeResponse>> updateFileMetadataRequestMono = updateFileMetadataRequest.flatMap(request -> {
            log.debug(Constant.INVOKED_METHOD + " - {} - {} - {} - {}", "updateMetadata", fileKey, xPagopaSafestorageCxId, request, pagopaSafestorageCxIdValue, apiKeyValue);
            return fileMetadataUpdateService.updateMetadata(fileKey,
                    xPagopaSafestorageCxId,
                    request,
                    pagopaSafestorageCxIdValue,
                    apiKeyValue);
        }).map(ResponseEntity::ok);
        log.info(Constant.ENDING_PROCESS_ON, UPDATE_FILE_METADATA, fileKey);
        return updateFileMetadataRequestMono;
    }
}
