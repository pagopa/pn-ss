package it.pagopa.pnss.uribuilder.rest;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;

import it.pagopa.pn.template.rest.v1.api.FileDownloadApi;
import it.pagopa.pn.template.rest.v1.dto.FileDownloadResponse;
import it.pagopa.pnss.uribuilder.service.UriBuilderService;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@RestController
@Slf4j
public class FileDownloadApiController implements FileDownloadApi {

    private final UriBuilderService uriBuilderService;

    public FileDownloadApiController(UriBuilderService uriBuilderService) {
        this.uriBuilderService = uriBuilderService;
    }

    @Override
    public Mono<ResponseEntity<FileDownloadResponse>> getFile(String fileKey, String xPagopaSafestorageCxId, Boolean metadataOnly,
                                                              final ServerWebExchange exchange) {
    	log.info("FileDownloadApiController.getFile() : START");
        return uriBuilderService.createUriForDownloadFile(fileKey, xPagopaSafestorageCxId, metadataOnly).map(ResponseEntity::ok);
    }
}
