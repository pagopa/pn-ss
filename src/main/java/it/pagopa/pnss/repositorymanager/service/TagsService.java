package it.pagopa.pnss.repositorymanager.service;

import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.TagsChanges;
import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.TagsDto;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

public interface TagsService {

    Mono<TagsDto> getTags(String tagKeyValue);

    Mono<Map<String, List<String>>> updateTags(String documentKey, TagsChanges tagsChanges);

}
