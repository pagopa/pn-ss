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
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.ssm.SsmAsyncClient;

import javax.annotation.PostConstruct;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static it.pagopa.pnss.common.utils.LogUtils.*;

/**
 * The main configuration class for the indexing service. It uses the Ssm client to get the json string parameter, with the settings for the indexing service, and parse it.
 * It even contains the valid tags for documents, generic methods to consult them and the limits to apply to the indexing service.
 */
@Configuration
@CustomLog
@ConditionalOnProperty(name = "pn.ss.indexing.configuration.test", havingValue = "false", matchIfMissing = true)
public class IndexingConfiguration {

    private final SsmAsyncClient ssmAsyncClient;
    private final JsonUtils jsonUtils;
    private final String indexingConfigurationName;
    private final ConcurrentMap<String, IndexingTag> tags;
    private IndexingLimits indexingLimits;

    /**
     * Instantiates a new Indexing configuration.
     *
     * @param ssmAsyncClient            the ssm async client
     * @param jsonUtils                 the json utils
     * @param indexingConfigurationName the json parameter name containing the indexing configuration
     */
    public IndexingConfiguration(SsmAsyncClient ssmAsyncClient, JsonUtils jsonUtils, @Value("${pn.ss.indexing.configuration.name}") String indexingConfigurationName) {
        this.ssmAsyncClient = ssmAsyncClient;
        this.jsonUtils = jsonUtils;
        this.indexingConfigurationName = indexingConfigurationName;
        this.tags = new ConcurrentHashMap<>();
    }

    /**
     * Init method to initialize the configuration.
     */
    @PostConstruct
    public void init() {
        log.info(INITIALIZING, INDEXING_CONFIGURATION);
        Mono.fromCompletionStage(() -> ssmAsyncClient.getParameter(builder -> builder.name(indexingConfigurationName).build()))
                .map(response -> jsonUtils.convertJsonStringToObject(response.parameter().value(), IndexingSettings.class))
                .flatMapMany(indexingSettings -> {
                    this.indexingLimits = indexingSettings.getLimits();
                    Flux<IndexingTag> globals = Flux.fromIterable(indexingSettings.getGlobals()).doOnNext(tag -> tag.setGlobal(true));
                    // "global" flag is set to false by default
                    Flux<IndexingTag> locals = Flux.fromIterable(indexingSettings.getLocals());
                    return Flux.merge(globals, locals);
                })
                .doOnNext(indexingTag -> tags.put(indexingTag.getKey(), indexingTag))
                .doOnError(throwable -> log.error(EXCEPTION_DURING_INITIALIZATION, INDEXING_CONFIGURATION, throwable))
                .blockLast();
    }

    /**
     * Method to check if a tag is valid. A tag is considered valid if it exists in the tags map.
     *
     * @param tagKey the tag key
     * @return the boolean
     */
    public boolean isTagValid(String tagKey) {
        return tags.containsKey(tagKey);
    }

    /**
     * Method to check if a tag is global. If it is not global, it means that it is a local tag.
     * If the tag does not exist, it will throw an exception.
     *
     * @param tagKey the tag key
     * @return the boolean
     * @throws MissingTagException the missing tag exception
     */
    public boolean isTagGlobal(String tagKey) {
        if (!isTagValid(tagKey)) {
            throw new MissingTagException(tagKey);
        }
        return tags.get(tagKey).isGlobal();
    }

    /**
     * Gets the info related to a tag with the given key. If the tag does not exist, it will throw an exception.
     *
     * @param tagKey the tag key
     * @return the tag info
     * @throws MissingTagException the missing tag exception
     */
    public IndexingTag getTagInfo(String tagKey) {
        if (!isTagValid(tagKey)) {
            throw new MissingTagException(tagKey);
        }
        return tags.get(tagKey);
    }

    /**
     * Method to get the indexing limits options.
     *
     * @return the indexing limits
     */
    public IndexingLimits getIndexingLimits() {
        return this.indexingLimits;
    }

    /**
     * Method to get the indexing tags.
     *
     * @return the tags
     */
    public ConcurrentMap<String, IndexingTag> getTags() {
        return this.tags;
    }

}
