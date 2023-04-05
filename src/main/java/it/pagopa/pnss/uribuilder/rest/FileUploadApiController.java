package it.pagopa.pnss.uribuilder.rest;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;

import it.pagopa.pn.template.rest.v1.api.FileUploadApi;
import it.pagopa.pn.template.rest.v1.dto.FileCreationRequest;
import it.pagopa.pn.template.rest.v1.dto.FileCreationResponse;
import it.pagopa.pnss.common.client.exception.ChecksumException;
import it.pagopa.pnss.uribuilder.service.UriBuilderService;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@RestController
@Slf4j
public class FileUploadApiController implements FileUploadApi {
	
	@Value("${header.x-checksum-value:#{null}}")
	private String headerXChecksumValue;
	
    private final UriBuilderService uriBuilderService;

    public FileUploadApiController(UriBuilderService uriBuilderService) {
        this.uriBuilderService = uriBuilderService;
    }


    @Override
    public Mono<ResponseEntity<FileCreationResponse>> createFile(String xPagopaSafestorageCxId,
                                                                 Mono<FileCreationRequest> fileCreationRequest,
                                                                 final ServerWebExchange exchange) {

        return fileCreationRequest.flatMap(request -> {
        								String checksumValue = null;

//										old version
//										if (request != null && request.getChecksumValue() != null && !request.getChecksumValue().isBlank()) {
//											checksumValue = request.getChecksumValue();
//										}
//										else if (exchange != null && exchange.getRequest() != null
//												&& headerXChecksumValue != null && !headerXChecksumValue.isBlank()
//												&& exchange.getRequest().getHeaders() !=null
//												&& exchange.getRequest().getHeaders().containsKey(headerXChecksumValue)) {
//											checksumValue = exchange.getRequest().getHeaders().getFirst(headerXChecksumValue);
//										}
//										if (checksumValue == null || checksumValue.isBlank()) {
//											return Mono.error(new ChecksumException("Checksum value (header or in request) not present"));
//										}

										if (headerXChecksumValue != null && !headerXChecksumValue.isBlank()) {
											if (exchange.getRequest().getHeaders().containsKey(headerXChecksumValue)) {
												checksumValue = exchange.getRequest().getHeaders().getFirst(headerXChecksumValue);
											}
											if (checksumValue == null || checksumValue.isBlank()) {
												return Mono.error(new ChecksumException("Checksum value (header or in request) not present"));
											}
										}
        								return uriBuilderService.createUriForUploadFile(xPagopaSafestorageCxId,
        																				request,
        																				checksumValue);
        						  })
        						  .onErrorResume(ChecksumException.class, throwable -> {
        							  log.error("FileUploadApiController.createFile() : errore checksum = {}", throwable.getMessage(), throwable);
        							  throw new ResponseStatusException(HttpStatus.BAD_REQUEST,throwable.getMessage());
        						  })
                                  .map(ResponseEntity::ok);
    }
}
