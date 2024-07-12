package it.pagopa.pnss.indexing.rest;

import it.pagopa.pn.safestorage.generated.openapi.server.v1.api.AdditionalFileTagsApi;
import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.AdditionalFileTagsUpdateRequest;
import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.AdditionalFileTagsUpdateResponse;
import lombok.CustomLog;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@RestController
@CustomLog
public class AdditionalFileTagsController implements AdditionalFileTagsApi {

    @Override
    public Mono<ResponseEntity<AdditionalFileTagsUpdateResponse>> additionalFileTagsUpdate(String fileKey, Mono<AdditionalFileTagsUpdateRequest> additionalFileTagsUpdateRequest, ServerWebExchange exchange) {
        return AdditionalFileTagsApi.super.additionalFileTagsUpdate(fileKey, additionalFileTagsUpdateRequest, exchange);
    }
}
