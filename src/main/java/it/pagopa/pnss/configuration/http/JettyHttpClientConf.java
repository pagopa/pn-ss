package it.pagopa.pnss.configuration.http;

import lombok.CustomLog;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.http.HttpField;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_XML_VALUE;

@Configuration
@CustomLog
public class JettyHttpClientConf {

    @Value("${jetty.maxConnectionsPerDestination}")
    private int maxConnections;
    private final SslContextFactory.Client sslContextFactory = new SslContextFactory.Client();
    private static final List<String> CONTENT_TYPE_OF_RESPONSE_BODY_TO_LOG = List.of(APPLICATION_JSON_VALUE, APPLICATION_XML_VALUE);

    @Bean
    public HttpClient getJettyHttpClient() {
        HttpClient myHC = new HttpClient(sslContextFactory) {
            @Override
            public Request newRequest(URI uri) {
                Request request = super.newRequest(uri);
                return enhance(request);
            }
        };
        myHC.setMaxConnectionsPerDestination(maxConnections);
        return myHC;
    }

    private Request enhance(Request request) {

        request.onRequestBegin(theRequest -> log.debug("Start {} request to {}", theRequest.getMethod(), theRequest.getURI()));

        request.onRequestContent((theRequest, content) -> log.debug("Request body --> {}", decodeContent(content)));

        request.onResponseContent((theResponse, content) -> {
            if (CONTENT_TYPE_OF_RESPONSE_BODY_TO_LOG.contains(theResponse.getHeaders().get(CONTENT_TYPE))) {
                log.debug("Response body --> {}", decodeContent(content));
            }
        });

        return request;
    }

    private String decodeContent(ByteBuffer content) {
        byte[] bytes = new byte[content.remaining()];
        content.get(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }

}
