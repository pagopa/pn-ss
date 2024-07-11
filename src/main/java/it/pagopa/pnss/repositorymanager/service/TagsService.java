package it.pagopa.pnss.repositorymanager.service;

import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.TagsChanges;
import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.TagsDto;
import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.TagsRelationsDto;
import reactor.core.publisher.Mono;

public interface TagsService {

    Mono<TagsRelationsDto> getTagsRelations(String tagKeyValue);

    Mono<TagsDto> updateTags(String documentKey, TagsChanges tagsChanges);

}
