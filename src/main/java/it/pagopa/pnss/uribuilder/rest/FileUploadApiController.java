package it.pagopa.pnss.uribuilder.rest;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;

import it.pagopa.pn.template.rest.v1.api.FileUploadApi;
import it.pagopa.pn.template.rest.v1.dto.FileCreationRequest;
import it.pagopa.pn.template.rest.v1.dto.FileCreationResponse;
import it.pagopa.pnss.uribuilder.service.UriBuilderService;
import reactor.core.publisher.Mono;

@RestController
public class FileUploadApiController implements FileUploadApi {
	
    private final UriBuilderService uriBuilderService;

    public FileUploadApiController(UriBuilderService uriBuilderService) {
        this.uriBuilderService = uriBuilderService;
    }


    @Override
    public Mono<ResponseEntity<FileCreationResponse>> createFile(String xPagopaSafestorageCxId,
                                                                 Mono<FileCreationRequest> fileCreationRequest,
                                                                 final ServerWebExchange exchange) {
    	
        return fileCreationRequest.flatMap(request -> uriBuilderService.createUriForUploadFile(xPagopaSafestorageCxId, 
        																						request))
                                  .map(ResponseEntity::ok);
    }
}
