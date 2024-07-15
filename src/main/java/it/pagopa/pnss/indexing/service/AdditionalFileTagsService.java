package it.pagopa.pnss.indexing.service;

import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.AdditionalFileTagsDto;
import reactor.core.publisher.Mono;

public interface AdditionalFileTagsService {

    Mono<AdditionalFileTagsDto> getDocumentTags(String fileKey, String clientId);

}
