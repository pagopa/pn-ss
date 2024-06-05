package it.pagopa.pnss.configuration;

import it.pagopa.pnss.common.service.IgnoredUpdateMetadataHandler;
import it.pagopa.pnss.transformation.service.S3Service;
import lombok.CustomLog;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.*;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;

import javax.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static it.pagopa.pnss.common.utils.LogUtils.*;

/**
 * A configuration class to download the list of files to ignore from S3 and store it in a cached map.
 * The class handles a scheduled task to periodically update this list.
 */
@CustomLog
@EnableScheduling
@Configuration
@ConditionalOnExpression("T(org.apache.commons.lang3.StringUtils).isNotEmpty('${pn.ss.ignored.update.metadata.list}')")
public class IgnoredUpdateMetadataConfig {
    private final IgnoredUpdateMetadataHandler ignoredUpdateMetadataHandler;
    private final S3Service s3Service;
    private Instant lastModified;
    private final String bucketName;
    private final String ignoredUpdateMetadataFileName;
    private static final String S3_URI_REGEX = "^s3://([^/]+)/(.+)$";

    /**
     * Instantiates a new Ignored update metadata config.
     *
     * @param ignoredUpdateMetadataListUri the s3 uri pointing to the list of files to ignore
     * @param ignoredUpdateMetadataHandler  the handler of the set containing files to ignore
     * @param s3Service                    the s3 service
     */
    public IgnoredUpdateMetadataConfig(@Value("${pn.ss.ignored.update.metadata.list}") String ignoredUpdateMetadataListUri, IgnoredUpdateMetadataHandler ignoredUpdateMetadataHandler, S3Service s3Service) {
        this.ignoredUpdateMetadataHandler = ignoredUpdateMetadataHandler;
        this.s3Service = s3Service;
        this.lastModified = Instant.now().minusSeconds(1);

        //Parse S3 URI
        Pattern pattern = Pattern.compile(S3_URI_REGEX);
        Matcher matcher = pattern.matcher(ignoredUpdateMetadataListUri);
        if (matcher.matches()) {
            this.bucketName = matcher.group(1);
            this.ignoredUpdateMetadataFileName = matcher.group(2);
        } else {
            throw new IllegalArgumentException("Invalid ignoredUpdateMetadataListUri: " + ignoredUpdateMetadataListUri);
        }
    }

    /**
     * Init method to initialize the configuration class.
     */
    @PostConstruct
    void init() {
        log.debug(INITIALIZING, IGNORED_UPDATE_METADATA_CONFIG);
        parseIgnoredUpdateMetadataList()
                .doOnNext(ignoredUpdateMetadataHandler::addFileKey)
                .doOnError(throwable -> log.warn(EXCEPTION_DURING_INITIALIZATION, IGNORED_UPDATE_METADATA_CONFIG, throwable))
                .blockLast();
    }

    /**
     * A scheduled task to periodically refresh the list of files to ignore.
     */
    @Scheduled(cron = "0 */5 * * * *")
    void refreshIgnoredUpdateMetadataListScheduled() {
        log.logStartingProcess(REFRESH_IGNORED_UPDATE_METADATA_LIST_SCHEDULED);
        refreshIgnoredUpdateMetadataList()
                .doOnError(throwable -> log.logEndingProcess(REFRESH_IGNORED_UPDATE_METADATA_LIST_SCHEDULED, false, throwable.getMessage()))
                .doOnSuccess(result -> log.logEndingProcess(REFRESH_IGNORED_UPDATE_METADATA_LIST_SCHEDULED))
                .subscribe();
    }

    /**
     * A method to refresh the list of files to ignore.
     * It first checks if the file has been modified, and if so, it refreshes the list.
     *
     * @return the mono containing the size of the updated list
     */
    Mono<Integer> refreshIgnoredUpdateMetadataList() {
        log.debug(INVOKING_METHOD_WITHOUT_ARGS, REFRESH_IGNORED_UPDATE_METADATA_LIST);
        return s3Service.headObject(ignoredUpdateMetadataFileName, bucketName)
                .transform(ignoreNoSuchBucketOrKeyException())
                .map(HeadObjectResponse::lastModified)
                .flatMapMany(s3LastModified -> {
                    //Scarica il file solo se è stato modificato dall'ultima schedulazione
                    if (s3LastModified != null && s3LastModified.isAfter(this.lastModified)) {
                        log.debug("The file has been modified, refreshing ignored update metadata list...");
                        this.lastModified = s3LastModified;
                        return parseIgnoredUpdateMetadataList();
                    } else {
                        log.debug("The file has not been modified, nothing to do...");
                        return Flux.empty();
                    }
                })
                .reduce(new ConcurrentHashMap<String, Boolean>(), (map, line) -> {
                    map.put(line, Boolean.TRUE);
                    return map;
                })
                .map(Map::keySet)
                .filter(keySet -> !keySet.isEmpty())
                .map(ignoredUpdateMetadataHandler::updateSet)
                .doOnNext(size -> log.debug("Updated ignoredUpdateMetadataSet, new size: {}", size));
    }

    /**
     * A method to download the list of files to ignore from S3 and parse it into a Flux of strings.
     *
     * @return the flux containing the list of files to ignore
     */
    private Flux<String> parseIgnoredUpdateMetadataList() {
        log.debug(INVOKING_METHOD_WITHOUT_ARGS, PARSE_IGNORED_UPDATE_METADATA_LIST);
        return s3Service.getObject(ignoredUpdateMetadataFileName, bucketName)
                .transform(ignoreNoSuchBucketOrKeyException())
                .map(ResponseBytes::asInputStream)
                .map(InputStreamReader::new)
                .map(BufferedReader::new)
                .flatMapIterable(bufferedReader -> bufferedReader.lines().toList());
    }

    /**
     * A function to ignore NoSuchBucketException and NoSuchKeyException.
     *
     * @param <T>
     */
    private <T> Function<Mono<T>, Mono<T>> ignoreNoSuchBucketOrKeyException() {
        return tMono -> tMono.onErrorResume(throwable -> throwable instanceof NoSuchKeyException || throwable instanceof NoSuchBucketException, throwable -> {
            log.warn("Ignoring exception in {}: {}", IGNORED_UPDATE_METADATA_CONFIG, throwable.getMessage());
            return Mono.empty();
        });
    }

}
