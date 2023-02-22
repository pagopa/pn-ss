package it.pagopa.pnss.uribuilder.rest;

import it.pagopa.pn.template.rest.v1.api.FileDownloadApi;
import it.pagopa.pn.template.rest.v1.api.FileMetadataUpdateApi;
import it.pagopa.pn.template.rest.v1.dto.FileDownloadResponse;
import it.pagopa.pn.template.rest.v1.dto.OperationResultCodeResponse;
import it.pagopa.pn.template.rest.v1.dto.UpdateFileMetadataRequest;
import it.pagopa.pnss.uribuilder.service.FileMetadataUpdateService;
import it.pagopa.pnss.uribuilder.service.UriBuilderService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@RestController
@RequiredArgsConstructor
public class FileMetadataUpdateApiController implements FileMetadataUpdateApi {

    @Autowired
    FileMetadataUpdateService service;


    @Override
    public Mono<ResponseEntity<OperationResultCodeResponse>> updateFileMetadata
            (String fileKey, String xPagopaSafestorageCxId, Mono<UpdateFileMetadataRequest> updateFileMetadataRequest, final ServerWebExchange exchange) {

        return updateFileMetadataRequest.flatMap(request -> service.createUriForUploadFile(fileKey,xPagopaSafestorageCxId, request))
                .map(ResponseEntity::ok);


    }
}
