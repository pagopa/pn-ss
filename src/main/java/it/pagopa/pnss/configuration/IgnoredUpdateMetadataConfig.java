package it.pagopa.pnss.configuration;

import it.pagopa.pnss.transformation.service.S3Service;
import lombok.CustomLog;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;
import reactor.core.publisher.Flux;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.Instant;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static it.pagopa.pnss.common.utils.LogUtils.*;

@CustomLog
@Configuration
@ConditionalOnExpression("T(org.apache.commons.lang3.StringUtils).isNotEmpty('${pn.ss.ignored.update.metadata.list}')")
public class IgnoredUpdateMetadataConfig {
    private final Set<String> ignoredUpdateMetadataSet;
    private final S3Service s3Service;
    private final String bucketName;
    private final String ignoredUpdateMetadataFileName;
    private Instant lastModified;
    private final String regex = "^s3://([^/]+)/(.+)$";

    public IgnoredUpdateMetadataConfig(@Value("${pn.ss.ignored.update.metadata.list}") String ignoredUpdateMetadataListUri, S3Service s3Service) {
        this.s3Service = s3Service;
        this.ignoredUpdateMetadataSet = ConcurrentHashMap.newKeySet();
        this.lastModified = Instant.now();
        //Parse S3 URI
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(ignoredUpdateMetadataListUri);
        if (matcher.matches()) {
            this.bucketName = matcher.group(1);
            this.ignoredUpdateMetadataFileName = matcher.group(2);
        } else {
            throw new IllegalArgumentException("Invalid ignoredUpdateMetadataListUri: " + ignoredUpdateMetadataListUri);
        }
    }

    //TODO Fix test issues.
//    @PostConstruct
//    void init() {
//        log.debug(INITIALIZING, IGNORED_UPDATE_METADATA_CONFIG);
//        parseIgnoredUpdateMetadataList()
//                .doOnError(throwable -> log.warn(EXCEPTION_DURING_INITIALIZATION, IGNORED_UPDATE_METADATA_CONFIG, throwable))
//                .blockLast();
//    }

    @Scheduled(cron = "0 */5 * * * *")
    void refreshIgnoredUpdateMetadataListScheduled() {
        log.logStartingProcess(REFRESH_IGNORED_UPDATE_METADATA_LIST_SCHEDULED);
        refreshIgnoredUpdateMetadataList()
                .doOnError(throwable -> log.logEndingProcess(REFRESH_IGNORED_UPDATE_METADATA_LIST_SCHEDULED, false, throwable.getMessage()))
                .doOnComplete(() -> log.logEndingProcess(REFRESH_IGNORED_UPDATE_METADATA_LIST_SCHEDULED))
                .blockLast();
    }

    public boolean isToIgnore(String fileKey) {
        return ignoredUpdateMetadataSet.contains(fileKey);
    }

    protected Flux<String> refreshIgnoredUpdateMetadataList() {
        log.debug(INVOKING_METHOD_WITHOUT_ARGS, REFRESH_IGNORED_UPDATE_METADATA_LIST);
        return s3Service.headObject(ignoredUpdateMetadataFileName, bucketName)
                .map(HeadObjectResponse::lastModified)
                .flatMapMany(lastModified -> {
                    //Scarica il file solo se è stato modificato dall'ultima schedulazione
                    if (lastModified != null && lastModified.isAfter(this.lastModified)) {
                        log.debug("The file has been modified, refreshing ignored update metadata list...");
                        this.lastModified = lastModified;
                        return parseIgnoredUpdateMetadataList();
                    } else {
                        log.debug("The file has not been modified, nothing to do...");
                        return Flux.empty();
                    }
                });
    }

    private Flux<String> parseIgnoredUpdateMetadataList() {
        log.debug(INVOKING_METHOD_WITHOUT_ARGS, PARSE_IGNORED_UPDATE_METADATA_LIST);
        return s3Service.getObject(ignoredUpdateMetadataFileName, bucketName)
                .map(ResponseBytes::asInputStream)
                .map(InputStreamReader::new)
                .map(BufferedReader::new)
                .flatMapIterable(bufferedReader -> bufferedReader.lines().toList())
                .doOnNext(ignoredUpdateMetadataSet::add);
    }

}
