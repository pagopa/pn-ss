package it.pagopa.pnss.configuration.security;

import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.UserConfigurationResponse;
import it.pagopa.pnss.common.client.UserConfigurationClientCall;
import it.pagopa.pnss.common.client.exception.IdClientNotFoundException;
import lombok.CustomLog;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.AuthenticationWebFilter;
import org.springframework.security.web.server.authentication.ServerAuthenticationConverter;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.security.config.web.server.SecurityWebFiltersOrder.AUTHENTICATION;

@Configuration
@CustomLog
@EnableWebFluxSecurity
public class SecurityConfiguration {

    private final UserConfigurationClientCall userConfigurationClientCall;

    @Value("${header.x-api-key}")
    private String xApiKey;

    @Value("${header.x-pagopa-safestorage-cx-id}")
    private String xPagopaSafestorageCxId;

    public SecurityConfiguration(UserConfigurationClientCall userConfigurationClientCall) {
        this.userConfigurationClientCall = userConfigurationClientCall;
    }

    @Bean
    public ReactiveAuthenticationManager reactiveAuthenticationManager() {
        return authentication -> Mono.fromSupplier(() -> {
            if (authentication != null && authentication.getCredentials() != null) {
                authentication.setAuthenticated(true);
            }
            return authentication;
        });
    }

    private Mono<KeyAuthenticationToken> analyze(HttpHeaders headerValues) {
        String pagopaSafestorageCxId = headerValues.getFirst(xPagopaSafestorageCxId);
        String apiKey = headerValues.getFirst(xApiKey);
        if ((pagopaSafestorageCxId != null && !pagopaSafestorageCxId.isEmpty())) {
            return userConfigurationClientCall.getUser(pagopaSafestorageCxId)
                    .onErrorResume(IdClientNotFoundException.class
                            , throwable -> {
                                log.debug("IdClientNotFoundException {} - {}", pagopaSafestorageCxId, (apiKey == null ? "null" : apiKey));
                                return Mono.error(new ResponseStatusException(FORBIDDEN, String.format("Invalid %s header", xPagopaSafestorageCxId)));
                            })
                    .flatMap(userConfigurationResponse -> getKeyAuthenticationTokenMono(userConfigurationResponse, apiKey, pagopaSafestorageCxId));
        } else {
            return Mono.empty();
        }
    }

    private Mono<KeyAuthenticationToken> getKeyAuthenticationTokenMono(UserConfigurationResponse userConfigurationResponse, String apiKey, String pagopaSafestorageCxId) {
        String testApiKey = (apiKey == null || apiKey.isEmpty()) ? "" : apiKey;
        if (!testApiKey.isEmpty() && !userConfigurationResponse.getUserConfiguration().getApiKey().equals(testApiKey)) {
            log.debug("apiKey not match {} - {} - {}", pagopaSafestorageCxId, (apiKey == null ? "null" : apiKey), testApiKey);
            return Mono.error(new ResponseStatusException(FORBIDDEN, String.format("Invalid %s header", xApiKey)));
        }
        return Mono.just(new KeyAuthenticationToken(testApiKey, pagopaSafestorageCxId));
    }


    @Bean
    public ServerAuthenticationConverter serverAuthenticationConverter() {
        return exchange -> Mono.justOrEmpty(exchange)
                               .flatMap(serverWebExchange -> Mono.justOrEmpty(serverWebExchange.getRequest().getHeaders()))
                               .flatMap(this::analyze);
    }

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http,
                                                         ReactiveAuthenticationManager reactiveAuthenticationManager,
                                                         ServerAuthenticationConverter serverAuthenticationConverter) {

        final AuthenticationWebFilter authenticationWebFilter = new AuthenticationWebFilter(reactiveAuthenticationManager);
        authenticationWebFilter.setServerAuthenticationConverter(serverAuthenticationConverter);

        return http.authorizeExchange()
                   .anyExchange()
                   .permitAll()
                   .and()
                   .addFilterAt(authenticationWebFilter, AUTHENTICATION)
                   .httpBasic()
                   .disable()
                   .csrf()
                   .disable()
                   .formLogin()
                   .disable()
                   .logout()
                   .disable()
                   .build();
    }
}
