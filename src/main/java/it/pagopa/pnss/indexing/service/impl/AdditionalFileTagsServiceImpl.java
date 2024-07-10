package it.pagopa.pnss.indexing.service.impl;

import it.pagopa.pnss.common.client.TagsClientCall;
import it.pagopa.pnss.indexing.service.AdditionalFileTagsService;
import lombok.CustomLog;
import org.springframework.stereotype.Service;

@Service
@CustomLog
public class AdditionalFileTagsServiceImpl implements AdditionalFileTagsService {

    private final TagsClientCall tagsClientCall;

    public AdditionalFileTagsServiceImpl(TagsClientCall tagsClientCall) {
        this.tagsClientCall = tagsClientCall;
    }
}
