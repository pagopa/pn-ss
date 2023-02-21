package it.pagopa.pnss.uribuilder.rest;

import it.pagopa.pn.template.rest.v1.api.FileDownloadApi;
import it.pagopa.pn.template.rest.v1.dto.FileDownloadResponse;
import it.pagopa.pnss.uribuilder.service.UriBuilderService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@RestController
public class FileDownloadApiController implements FileDownloadApi {

    private final UriBuilderService uriBuilderService;

    public FileDownloadApiController(UriBuilderService uriBuilderService) {
        this.uriBuilderService = uriBuilderService;
    }

    @Override
    public Mono<ResponseEntity<FileDownloadResponse>> getFile(String fileKey, String xPagopaSafestorageCxId, Boolean metadataOnly,
                                                              final ServerWebExchange exchange) {

        return uriBuilderService.createUriForDownloadFile(fileKey, xPagopaSafestorageCxId, metadataOnly).map(ResponseEntity::ok);
    }
}
