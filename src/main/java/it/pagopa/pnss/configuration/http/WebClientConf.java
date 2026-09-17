package it.pagopa.pnss.configuration.http;

import it.pagopa.pnss.configurationproperties.PnSsConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.JettyClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Configuration
public class WebClientConf {

    private final JettyHttpClientConf jettyHttpClientConf;

    private final String internalBaseUrl;
    private final String corrIdHeaderName;

	public WebClientConf(JettyHttpClientConf jettyHttpClientConf, PnSsConfig pnSsConfig) {
        this.jettyHttpClientConf = jettyHttpClientConf;
        this.internalBaseUrl = pnSsConfig.getClientInterni().getBaseUrl();
        this.corrIdHeaderName = pnSsConfig.getClientInterni().getHeader().getCorrelationId();
    }

    private WebClient.Builder defaultWebClientBuilder() {
        return WebClient.builder().clientConnector(new JettyClientHttpConnector(jettyHttpClientConf.getJettyHttpClient()));
    }

    private WebClient.Builder defaultJsonWebClientBuilder() {
        return defaultWebClientBuilder().defaultHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE);
    }

    @Bean
    public WebClient stateMachineWebClient(PnSsConfig pnSsConfig) {
        return defaultJsonWebClientBuilder().baseUrl(pnSsConfig.getEndpoint().getStateMachine().getContainerBaseUrl()).build();
    }

    @Bean
    public WebClient ssWebClient() {
        return defaultJsonWebClientBuilder().baseUrl(internalBaseUrl).build();
    }

    @Bean
    public WebClient genericWebClient() {
        return defaultJsonWebClientBuilder().build();
    }
}
