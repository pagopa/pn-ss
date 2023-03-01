package it.pagopa.pnss.uribuilder.rest;

import it.pagopa.pnss.uribuilder.service.UriBuilderService;
import it.pagopa.pn.template.rest.v1.api.FileUploadApi;
import it.pagopa.pn.template.rest.v1.dto.FileCreationRequest;
import it.pagopa.pn.template.rest.v1.dto.FileCreationResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@RestController
public class FileUploadApiController implements FileUploadApi {
	
    @Value("${header.x-api-key}")
    private String apiKey;

    @Value("${header.x-pagopa-safestorage-cx-id}")
    private String pagopaSafestorageCxId;

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
    
    /*
	   exchange.getRequest().getHeaders().getFirstDate(pagopaSafestorageCxId),
	   exchange.getRequest().getHeaders().getFirstDate(apiKey) */
}
