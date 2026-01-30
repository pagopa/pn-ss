package it.pagopa.pnss.configuration.http;

import it.pagopa.pn.commons.utils.MDCUtils;
import lombok.CustomLog;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.Request;
import org.eclipse.jetty.client.transport.HttpClientTransportDynamic;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.io.ClientConnector;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
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

        // Creare un ClientConnector e assegnare la SslContextFactory
        ClientConnector connector = new ClientConnector();
        connector.setSslContextFactory(sslContextFactory);

        // Creare il Trasporto usando il connector
        // HttpClientTransportDynamic supporta il passaggio automatico tra HTTP/1.1 e HTTP/2
        HttpClientTransportDynamic transport = new HttpClientTransportDynamic(connector);
        // Creare l'HttpClient passando il trasporto
        HttpClient myHC = new HttpClient(transport) {
            @Override
            public Request newRequest(URI uri) {
                Request request = super.newRequest(uri);
                return enhance(request, MDCUtils.retrieveMDCContextMap());
            }
        };
        myHC.setMaxConnectionsPerDestination(maxConnections);
        return myHC;
    }

//    private Request enhance(Request request, Map<String, String> mdcContextMap) {
//        return request.onRequestBegin(theRequest -> {
//                    MDCUtils.enrichWithMDC(request, mdcContextMap);
//                    if (mdcContextMap != null && mdcContextMap.containsKey(MDC_CORR_ID_KEY)) {
//                        request.header(corrIdHeaderName, MDC.get(MDC_CORR_ID_KEY));
//                    }
//                    log.debug("Start {} request to {}", theRequest.getMethod(), theRequest.getURI());
//                })
//                .onResponseContent((theResponse, content) -> {
//                    MDCUtils.enrichWithMDC(request, mdcContextMap);
//                    if (CONTENT_TYPE_OF_RESPONSE_BODY_TO_LOG.contains(theResponse.getHeaders().get(CONTENT_TYPE))) {
//                        log.debug("Response body --> {}", decodeContent(content));
//                    }
//                });
//    }

    private Request enhance(Request request, Map<String, String> mdcContextMap) {
        // Usiamo una lista per accumulare i vari pezzi (chunk) della risposta
        final List<ByteBuffer> chunks = new ArrayList<>();

        // Configurare l'inizio della richiesta
        request.onRequestBegin(theRequest -> {
            MDCUtils.enrichWithMDC(theRequest, mdcContextMap);
            if (mdcContextMap != null && mdcContextMap.containsKey(MDC_CORR_ID_KEY)) {
                theRequest.headers(h -> h.put(corrIdHeaderName, mdcContextMap.get(MDC_CORR_ID_KEY)));
            }
            log.debug("Start {} request to {}", theRequest.getMethod(), theRequest.getURI());
        });

        // Configurare la gestione del contenuto della risposta
        // Chiamata separata perché onRequestBegin restituisce void (o non restituisce la Request)
        request.onResponseContent((theResponse, content) -> {
            // Verifichiamo se il Content-Type è tra quelli da loggare
            String contentType = theResponse.getHeaders().get(HttpHeader.CONTENT_TYPE);
            if (contentType != null && CONTENT_TYPE_OF_RESPONSE_BODY_TO_LOG.contains(contentType)) {
                // Copiamo il chunk per evitare che Jetty lo riutilizzi prima del log finale
                ByteBuffer copy = ByteBuffer.allocate(content.remaining());
                copy.put(content.duplicate());
                copy.flip();
                chunks.add(copy);
            }
        });

        request.onResponseSuccess(theResponse -> {
            MDCUtils.enrichWithMDC(request, mdcContextMap);
            if (!chunks.isEmpty()) {
                // Uniamo tutti i chunk accumulati
                int totalSize = chunks.stream().mapToInt(ByteBuffer::remaining).sum();
                ByteBuffer fullBuffer = ByteBuffer.allocate(totalSize);
                chunks.forEach(fullBuffer::put);
                fullBuffer.flip();

                log.debug("Response body --> {}", decodeContent(fullBuffer));
                chunks.clear(); // Pulizia per sicurezza
            }
        });

        // Caso Fallimento: Logghiamo l'errore e puliamo i buffer
        request.onResponseFailure((theResponse, failure) -> {
            MDCUtils.enrichWithMDC(request, mdcContextMap);

            log.error("Request failed for {} {}. Error: {}",
                    request.getMethod(),
                    request.getURI(),
                    failure.getMessage());
            // Pulizia fondamentale per liberare la memoria dei chunk accumulati finora
            chunks.clear();
        });

        return request;
    }


//    private String decodeContent(ByteBuffer content) {
//        byte[] bytes = new byte[content.remaining()];
//        content.get(bytes);
//        return new String(bytes, StandardCharsets.UTF_8);
//    }

    private String decodeContent(ByteBuffer content) {
        if (content == null || !content.hasRemaining()) {
            return "";
        }
        // Usiamo duplicate per non alterare la posizione del buffer originale
        // utile se dovessi rileggerlo per altri scopi
        ByteBuffer bufferToRead = content.duplicate();
        byte[] bytes = new byte[bufferToRead.remaining()];
        bufferToRead.get(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }
}
