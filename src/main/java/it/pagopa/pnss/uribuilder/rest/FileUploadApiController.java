package it.pagopa.pnss.uribuilder.rest;

import it.pagopa.pnss.common.service.HeadersChecker;
import it.pagopa.pnss.uribuilder.service.UriBuilderService;
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

    UriBuilderService uriBuilderService;
    HeadersChecker headersChecker;

    @Autowired
    public FileUploadApiController(UriBuilderService uriBuilderService, HeadersChecker headersChecker) {
        this.uriBuilderService = uriBuilderService;
        this.headersChecker = headersChecker;
    }


    @Override
    public Mono<ResponseEntity<FileCreationResponse>> createFile(String xPagopaSafestorageCxId,
                                                                 Mono<FileCreationRequest> fileCreationRequest,
                                                                 final ServerWebExchange exchange) {
    	
        return headersChecker.checkIdentity(exchange)
        		.flatMap(unused -> fileCreationRequest)
        		.flatMap(request -> uriBuilderService.createUriForUploadFile(xPagopaSafestorageCxId, request))
                .map(ResponseEntity::ok);
    }
}
