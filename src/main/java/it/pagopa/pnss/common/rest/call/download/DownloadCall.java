package it.pagopa.pnss.common.rest.call.download;

import reactor.core.publisher.Mono;

public interface DownloadCall {

    Mono<byte[]> downloadFile(String url);
}
