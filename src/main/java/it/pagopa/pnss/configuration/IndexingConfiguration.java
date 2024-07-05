package it.pagopa.pnss.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pnss.common.exception.MissingIndexingLimitsException;
import it.pagopa.pnss.common.model.pojo.IndexingLimits;
import it.pagopa.pnss.common.model.pojo.IndexingSettings;
import it.pagopa.pnss.common.model.pojo.IndexingTag;
import it.pagopa.pnss.common.utils.JsonUtils;
import lombok.CustomLog;
import org.apache.commons.lang3.NotImplementedException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;
import software.amazon.awssdk.services.ssm.SsmAsyncClient;

import javax.annotation.PostConstruct;
import java.util.concurrent.ConcurrentMap;


import static it.pagopa.pnss.common.utils.LogUtils.INDEXING_CONFIGURATION;
import static it.pagopa.pnss.common.utils.LogUtils.INITIALIZING;

@Configuration
@CustomLog
@Validated
@ConditionalOnProperty(name = "pn.ss.indexing.configuration.test", havingValue = "false", matchIfMissing = true)
public class IndexingConfiguration {

    private final SsmAsyncClient ssmAsyncClient;
    private final JsonUtils jsonUtils;
    private final String indexingConfigurationName;
    private ConcurrentMap<String, IndexingTag> tags;
    private IndexingLimits indexingLimits;

    public IndexingConfiguration(SsmAsyncClient ssmAsyncClient, JsonUtils jsonUtils, @Value("${pn.ss.indexing.configuration.name}") String indexingConfigurationName) {
        this.ssmAsyncClient = ssmAsyncClient;
        this.jsonUtils = jsonUtils;
        this.indexingConfigurationName = indexingConfigurationName;
    }

    @PostConstruct
    public void init() {
        log.info(INITIALIZING, INDEXING_CONFIGURATION);
        //TODO Parse jsonParameter and initialize globalTags, localTags and indexingLimits
    }

    IndexingSettings parseJsonParameter(String jsonString) {
        return null;
    }

    public boolean isTagValid(String tagKey) {
        throw new NotImplementedException("Method not implemented");
    }

    public boolean isTagGlobal(String tagKey) {
        throw new NotImplementedException("Method not implemented");
    }

    public IndexingTag getTagInfo(String tagKey) {
        return null;
    }

    public IndexingLimits getIndexingLimits() {
        return null;
    }

    public ConcurrentMap<String, IndexingTag> getTags() {
        return tags;
    }

}
