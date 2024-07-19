package it.pagopa.pnss.indexing.service;

import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.AdditionalFileTagsDto;
import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.AdditionalFileTagsSearchResponseFileKeys;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.AdditionalFileTagsUpdateRequest;
import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.AdditionalFileTagsUpdateResponse;
import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.TagsChanges;

public interface AdditionalFileTagsService {

    Mono<AdditionalFileTagsDto> getDocumentTags(String fileKey, String clientId);

    Mono<List<AdditionalFileTagsSearchResponseFileKeys>> searchTags(String xPagopaSafestorageCxId, String logic, Boolean tags, Map<String, String> queryParams);

    Mono<AdditionalFileTagsUpdateResponse> postTags(String cxId, AdditionalFileTagsUpdateRequest request, String fileKey);

    Mono<Boolean> getPermission(String cxId);

    Mono<TagsChanges> requestValidation(AdditionalFileTagsUpdateRequest request, String cxId);
}
