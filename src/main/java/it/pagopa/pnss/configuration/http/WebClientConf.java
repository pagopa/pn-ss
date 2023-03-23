package it.pagopa.pnss.configuration.http;

import it.pagopa.pnss.common.configurationproperties.endpoint.internal.statemachine.StateMachineEndpointProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.JettyClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;

import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Configuration
public class WebClientConf {

    private final JettyHttpClientConf jettyHttpClientConf;

    public WebClientConf(JettyHttpClientConf jettyHttpClientConf) {
        this.jettyHttpClientConf = jettyHttpClientConf;
    }

    private WebClient.Builder defaultWebClientBuilder() {
        return WebClient.builder().clientConnector(new JettyClientHttpConnector(jettyHttpClientConf.getJettyHttpClient()));
    }
    private WebClient.Builder defaultJsonWebClientBuilder() {
        return defaultWebClientBuilder().defaultHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE);
    }
    @Bean
    public WebClient stateMachineWebClient(StateMachineEndpointProperties stateMachineEndpointProperties) {
        return defaultJsonWebClientBuilder().baseUrl(stateMachineEndpointProperties.containerBaseUrl()).build();
    }
}
