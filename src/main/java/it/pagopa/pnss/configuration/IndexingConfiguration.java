package it.pagopa.pnss.configuration;

import it.pagopa.pnss.common.exception.MissingTagException;
import it.pagopa.pnss.common.model.pojo.IndexingLimits;
import it.pagopa.pnss.common.model.pojo.IndexingSettings;
import it.pagopa.pnss.common.model.pojo.IndexingTag;
import it.pagopa.pnss.common.utils.JsonUtils;
import lombok.CustomLog;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.ssm.SsmAsyncClient;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static it.pagopa.pnss.common.utils.LogUtils.*;

@Configuration
@CustomLog
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
        Mono.fromCompletionStage(() -> ssmAsyncClient.getParameter(builder -> builder.name(indexingConfigurationName).build()))
                .map(response -> jsonUtils.convertJsonStringToObject(response.parameter().value(), IndexingSettings.class))
                .map(indexingSettings -> {
                    this.indexingLimits = indexingSettings.getLimits();
                    List<IndexingTag> globals = indexingSettings.getGlobals();
                    List<IndexingTag> locals = indexingSettings.getLocals();
                    globals.forEach(tag -> tag.setGlobal(true));
                    locals.forEach(tag -> tag.setGlobal(false));
                    this.tags = Stream.concat(globals.stream(), locals.stream()).collect(Collectors.toConcurrentMap(IndexingTag::getKey, tag -> tag));
                    log.info("Indexing tags: {}", this.tags);
                    return indexingSettings;
                })
                .doOnError(throwable -> log.error(EXCEPTION_DURING_INITIALIZATION, INDEXING_CONFIGURATION, throwable))
                .block();
    }

    public boolean isTagValid(String tagKey) {
        return tags.containsKey(tagKey);
    }

    public boolean isTagGlobal(String tagKey) {
        if (!isTagValid(tagKey)) {
            throw new MissingTagException(tagKey);
        }
        return tags.get(tagKey).isGlobal();
    }

    public IndexingTag getTagInfo(String tagKey) {
        if (!isTagValid(tagKey)) {
            throw new MissingTagException(tagKey);
        }
        return tags.get(tagKey);
    }

    public IndexingLimits getIndexingLimits() {
        return this.indexingLimits;
    }

    public ConcurrentMap<String, IndexingTag> getTags() {
        return this.tags;
    }

}
