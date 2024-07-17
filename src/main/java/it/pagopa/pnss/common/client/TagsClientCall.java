package it.pagopa.pnss.common.client;

import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.TagsRelationsResponse;
import reactor.core.publisher.Mono;

public interface TagsClientCall {
    Mono<TagsRelationsResponse> getTagsRelations(String tagKeyValue);

}
