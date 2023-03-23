package it.pagopa.pnss.configuration.http;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.URI;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.http.HttpField;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_XML_VALUE;


@Configuration
@Slf4j
public class JettyHttpClientConf {

    private final CharsetDecoder charsetDecoder = StandardCharsets.UTF_8.newDecoder();
    private final SslContextFactory.Client sslContextFactory = new SslContextFactory.Client();
    private static final List<String> CONTENT_TYPE_OF_RESPONSE_BODY_TO_LOG = List.of(APPLICATION_JSON_VALUE, APPLICATION_XML_VALUE);

    @Bean
    public HttpClient getJettyHttpClient() {
        return new HttpClient(sslContextFactory) {
            @Override
            public Request newRequest(URI uri) {
                Request request = super.newRequest(uri);
                return enhance(request);
            }
        };
    }

    private Request enhance(Request request) {

        request.onRequestBegin(theRequest -> log.info("Start {} request to {}", theRequest.getMethod(), theRequest.getURI()));

        request.onRequestHeaders(theRequest -> {
            for (HttpField header : theRequest.getHeaders()) {
                log.debug("Header {} --> {}", header.getName(), header.getValue());
            }
        });

        request.onRequestContent((theRequest, content) -> {
            try {
                log.info("Request body --> {}", charsetDecoder.decode(content));
            } catch (CharacterCodingException e) {
                log.error(e.getMessage(), e);
            }
        });

        request.onResponseContent((theResponse, content) -> {
            if (CONTENT_TYPE_OF_RESPONSE_BODY_TO_LOG.contains(theResponse.getHeaders().get(CONTENT_TYPE))) {
                try {
                    log.info("Response body --> {}", charsetDecoder.decode(content));
                } catch (CharacterCodingException e) {
                    log.error(e.getMessage(), e);
                }
            }
        });

        return request;
    }
}
