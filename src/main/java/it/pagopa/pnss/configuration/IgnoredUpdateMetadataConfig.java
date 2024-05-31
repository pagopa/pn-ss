package it.pagopa.pnss.configuration;

import it.pagopa.pnss.common.rest.call.download.DownloadCall;
import lombok.CustomLog;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Configuration
@CustomLog
public class IgnoredUpdateMetadataConfig {

    private final DownloadCall downloadCall;
    private final Set<String> ignoredUpdateMetadataSet;
    private final String ignoredUpdateMetadataListUri;
    public IgnoredUpdateMetadataConfig(DownloadCall downloadCall,
                                       @Value("${pn.ss.ignored.update.metadata.list}") String ignoredUpdateMetadataListUri) {
        this.downloadCall = downloadCall;
        this.ignoredUpdateMetadataListUri = ignoredUpdateMetadataListUri;
        this.ignoredUpdateMetadataSet = ConcurrentHashMap.newKeySet();
    }

    private void parseIgnoredUpdateMetadataList() {

        downloadCall.downloadFile(ignoredUpdateMetadataListUri)
                .map(ByteArrayInputStream::new)
                .map(InputStreamReader::new)
                .map(BufferedReader::new)
                .flatMapIterable(bufferedReader -> bufferedReader.lines().toList())
                .map(ignoredUpdateMetadataSet::add)
                .blockLast();
    }

}
