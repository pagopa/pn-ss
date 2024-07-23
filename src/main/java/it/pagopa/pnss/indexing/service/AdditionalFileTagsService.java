package it.pagopa.pnss.indexing.service;

import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.*;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

public interface AdditionalFileTagsService {

    Mono<AdditionalFileTagsDto> getDocumentTags(String fileKey, String clientId);

    Mono<AdditionalFileTagsMassiveUpdateResponse> postMassiveTags(AdditionalFileTagsMassiveUpdateRequest request, String cxId);

    Mono<List<AdditionalFileTagsSearchResponseFileKeys>> searchTags(String xPagopaSafestorageCxId, String logic, Boolean tags, Map<String, String> queryParams);

    Mono<AdditionalFileTagsUpdateResponse> postTags(String cxId, AdditionalFileTagsUpdateRequest request, String fileKey);

    Mono<Boolean> getWriteTagsPermission(String cxId);

    Mono<TagsChanges> requestValidation(AdditionalFileTagsUpdateRequest request, String cxId);
}
