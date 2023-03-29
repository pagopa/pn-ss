package it.pagopa.pnss.uribuilder.rest;

import it.pagopa.pn.template.rest.v1.api.FileMetadataUpdateApi;
import it.pagopa.pn.template.rest.v1.dto.OperationResultCodeResponse;
import it.pagopa.pn.template.rest.v1.dto.UpdateFileMetadataRequest;
import it.pagopa.pnss.uribuilder.service.FileMetadataUpdateService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@RestController
public class FileMetadataUpdateApiController implements FileMetadataUpdateApi {

    final FileMetadataUpdateService service;

    @Value("${header.x-api-key}")
    private String apiKey;

    @Value("${header.x-pagopa-safestorage-cx-id}")
    private String pagopaSafestorageCxId;

    public FileMetadataUpdateApiController(FileMetadataUpdateService service) {
        this.service = service;
    }

    @Override
    public Mono<ResponseEntity<OperationResultCodeResponse>> updateFileMetadata(String fileKey, String xPagopaSafestorageCxId,
                                                                                Mono<UpdateFileMetadataRequest> updateFileMetadataRequest
            , final ServerWebExchange exchange) {

        String pagopaSafestorageCxIdValue = exchange.getRequest().getHeaders().getFirst(pagopaSafestorageCxId);
        String apiKeyValue = exchange.getRequest().getHeaders().getFirst(apiKey);

        return updateFileMetadataRequest.flatMap(request -> service.createUriForUploadFile(fileKey,
                                                                                           xPagopaSafestorageCxId,
                                                                                           request,
                                                                                           pagopaSafestorageCxIdValue,
                                                                                           apiKeyValue)).map(ResponseEntity::ok);
    }
}
