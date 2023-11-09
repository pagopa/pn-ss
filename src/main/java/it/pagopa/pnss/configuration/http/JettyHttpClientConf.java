package it.pagopa.pnss.configuration.http;

import it.pagopa.pn.commons.utils.MDCUtils;
import lombok.CustomLog;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static it.pagopa.pnss.common.utils.LogUtils.MDC_CORR_ID_KEY;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_XML_VALUE;

@Configuration
@CustomLog
public class JettyHttpClientConf {

    @Value("${jetty.maxConnectionsPerDestination}")
    private int maxConnections;
    @Value("${pn.log.cx-id-header}")
    private String corrIdHeaderName;
    private final SslContextFactory.Client sslContextFactory = new SslContextFactory.Client();
    private static final List<String> CONTENT_TYPE_OF_RESPONSE_BODY_TO_LOG = List.of(APPLICATION_JSON_VALUE, APPLICATION_XML_VALUE);

    @Bean
    public HttpClient getJettyHttpClient() {
        HttpClient myHC = new HttpClient(sslContextFactory) {
            @Override
            public Request newRequest(URI uri) {
                Request request = super.newRequest(uri);
                return enhance(request, MDCUtils.retrieveMDCContextMap());
            }
        };
        myHC.setMaxConnectionsPerDestination(maxConnections);
        return myHC;
    }

    private Request enhance(Request request, Map<String, String> mdcContextMap) {
        return request.onRequestBegin(theRequest -> {
                    MDCUtils.enrichWithMDC(request, mdcContextMap);
                    if (mdcContextMap != null && mdcContextMap.containsKey(MDC_CORR_ID_KEY)) {
                        request.header(corrIdHeaderName, MDC.get(MDC_CORR_ID_KEY));
                    }
                    log.debug("Start {} request to {}", theRequest.getMethod(), theRequest.getURI());
                })
                .onRequestContent((theRequest, content) -> {
                    MDCUtils.enrichWithMDC(request, mdcContextMap);
                    log.debug("Request body --> {}", decodeContent(content));
                })
                .onResponseContent((theResponse, content) -> {
                    MDCUtils.enrichWithMDC(request, mdcContextMap);
                    if (CONTENT_TYPE_OF_RESPONSE_BODY_TO_LOG.contains(theResponse.getHeaders().get(CONTENT_TYPE))) {
                        log.debug("Response body --> {}", decodeContent(content));
                    }
                });
    }

    private String decodeContent(ByteBuffer content) {
        byte[] bytes = new byte[content.remaining()];
        content.get(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }

}
