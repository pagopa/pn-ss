package it.pagopa.pnss.uriBuilder.rest;

import it.pagopa.pnss.uriBuilder.service.UriBuilderService;
import it.pagopa.pn.template.rest.v1.api.FileUploadApi;
import it.pagopa.pn.template.rest.v1.dto.FileCreationRequest;
import it.pagopa.pn.template.rest.v1.dto.FileCreationResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.validation.constraints.NotNull;

public class FileUploadApiController implements FileUploadApi {

    @Autowired
    UriBuilderService uriBuilderService;


    @Override
    public Mono <ResponseEntity <FileCreationResponse>> createFile(String xPagopaSafestorageCxId, Mono<FileCreationRequest> fileCreationRequest, final ServerWebExchange exchange) {


        @NotNull String contentType = fileCreationRequest.block().getContentType();
        @NotNull String documentType = fileCreationRequest.block().getDocumentType();
        @NotNull String status = fileCreationRequest.block().getStatus();
        FileCreationResponse creationResp = uriBuilderService.createUriForUploadFile(contentType, documentType, status);

        Mono<ResponseEntity<FileCreationResponse>> result = Mono.justOrEmpty(ResponseEntity.ok().body(creationResp));
        return  result;

    }

}
