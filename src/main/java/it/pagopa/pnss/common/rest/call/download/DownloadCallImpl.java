package it.pagopa.pnss.common.rest.call.download;

import lombok.CustomLog;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import java.net.URI;

import static it.pagopa.pnss.common.utils.LogUtils.*;

@Component
@CustomLog
public class DownloadCallImpl implements DownloadCall {

    private final WebClient genericWebClient;

    public DownloadCallImpl(WebClient genericWebClient) {
        this.genericWebClient = genericWebClient;
    }

    @Override
    public Mono<byte[]> downloadFile(String url) {
        log.debug(CLIENT_METHOD_INVOCATION, DOWNLOAD_FILE, url);
        return genericWebClient.get().uri(URI.create(url))
                .retrieve()
                .bodyToMono(byte[].class)
                .doOnSuccess(result -> log.info(CLIENT_METHOD_RETURN, DOWNLOAD_FILE, url))
                .doOnError(e -> log.error("Error in downloadFile class: {}", e.getMessage()));
    }
}
