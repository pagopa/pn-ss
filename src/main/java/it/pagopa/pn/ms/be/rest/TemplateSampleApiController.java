package it.pagopa.pn.ms.be.rest;


import it.pagopa.pn.safestorage.generated.openapi.server.v1.api.TemplateSampleApi;
import it.pagopa.pnss.common.utils.LogUtils;
import lombok.CustomLog;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@CustomLog
@RestController
public class TemplateSampleApiController implements TemplateSampleApi {

    @Override
    public Mono<ResponseEntity<Map<String, List<String>>>> getHttpHeadersMap(ServerWebExchange exchange) {

        final String GET_HTTP_HEADERS_MAP = "getHttpHeadersMap";
        log.info(LogUtils.STARTING_PROCESS, GET_HTTP_HEADERS_MAP);

        return Mono.fromSupplier(() ->{
            Map<String, List<String>> headers = new HashMap<>();
            exchange.getRequest().getHeaders().forEach(headers::put);
            log.info(LogUtils.ENDING_PROCESS, GET_HTTP_HEADERS_MAP);
            return ResponseEntity.ok(headers);
        });

    }
}
