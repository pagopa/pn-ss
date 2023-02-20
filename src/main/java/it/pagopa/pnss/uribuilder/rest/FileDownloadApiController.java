package it.pagopa.pnss.uribuilder.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;

import it.pagopa.pn.template.rest.v1.api.FileDownloadApi;
import it.pagopa.pn.template.rest.v1.dto.FileDownloadResponse;
import it.pagopa.pnss.common.service.HeadersChecker;
import it.pagopa.pnss.uribuilder.service.UriBuilderService;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@RestController
@RequiredArgsConstructor
public class FileDownloadApiController implements FileDownloadApi {

    @Autowired
    UriBuilderService uriBuilderService;
    @Autowired
    HeadersChecker headersChecker;

    @Override
    public Mono <ResponseEntity <FileDownloadResponse>> getFile(String fileKey, String xPagopaSafestorageCxId, Boolean metadataOnly, final ServerWebExchange exchange) {

        return headersChecker.checkIdentity(exchange)
        		.flatMap(unused -> uriBuilderService.createUriForDownloadFile(fileKey,xPagopaSafestorageCxId,metadataOnly))
        		.map(ResponseEntity::ok);
    }
}
