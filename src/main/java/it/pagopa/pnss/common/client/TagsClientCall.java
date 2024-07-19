package it.pagopa.pnss.common.client;

import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.TagsRelationsResponse;
import reactor.core.publisher.Mono;
import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.TagsChanges;
import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.TagsResponse;

public interface TagsClientCall {
    Mono<TagsRelationsResponse> getTagsRelations(String tagKeyValue);

    Mono<TagsResponse> putTags(String documentKey, TagsChanges tagsChanges);

}
