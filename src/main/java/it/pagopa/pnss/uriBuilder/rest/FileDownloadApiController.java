package it.pagopa.pnss.uriBuilder.rest;

import it.pagopa.pnss.uriBuilder.service.UriBuilderService;
import it.pagopa.pn.template.rest.v1.api.FileDownloadApi;
import it.pagopa.pn.template.rest.v1.dto.FileDownloadResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@RestController
@RequiredArgsConstructor
public class FileDownloadApiController implements FileDownloadApi {



    @Autowired
    UriBuilderService uriBuilderService;

    @Override
    public Mono <ResponseEntity <FileDownloadResponse>> getFile(String fileKey, String xPagopaSafestorageCxId, Boolean metadataOnly, final ServerWebExchange exchange) {



        FileDownloadResponse response  = uriBuilderService.createUriForDownloadFile(fileKey);
        if (response ==null ){
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "File Not Found for fileKey: " + fileKey);
        }

        Mono<ResponseEntity<FileDownloadResponse>> result = Mono.justOrEmpty(ResponseEntity.ok().body(response));

        return  result;
    }
}
