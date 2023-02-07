package it.pagopa.pnss.uriBuilder.rest;

import it.pagopa.pnss.uriBuilder.service.UriBuilderService;
import it.pagopa.pn.template.rest.v1.api.FileUploadApi;
import it.pagopa.pn.template.rest.v1.dto.FileCreationRequest;
import it.pagopa.pn.template.rest.v1.dto.FileCreationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@RestController
@RequiredArgsConstructor
public class FileUploadApiController implements FileUploadApi {




    @Autowired
    UriBuilderService uriBuilderService;



    @Override
    public Mono<ResponseEntity<FileCreationResponse>> createFile(String xPagopaSafestorageCxId, Mono<FileCreationRequest> fileCreationRequest, final ServerWebExchange exchange){

        return fileCreationRequest.map(request ->uriBuilderService.createUriForUploadFile(xPagopaSafestorageCxId,request));

    }



}
