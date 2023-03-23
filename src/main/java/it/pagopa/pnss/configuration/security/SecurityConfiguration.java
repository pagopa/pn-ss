package it.pagopa.pnss.configuration.security;

import it.pagopa.pnss.common.client.UserConfigurationClientCall;
import it.pagopa.pnss.common.client.exception.IdClientNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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

    @Bean
    public ServerAuthenticationConverter serverAuthenticationConverter() {
        return exchange -> Mono.justOrEmpty(exchange)
                               .flatMap(serverWebExchange -> Mono.justOrEmpty(serverWebExchange.getRequest().getHeaders()))
                               .flatMap(headerValues -> {
                                   String apiKey = headerValues.getFirst(xApiKey);
                                   String pagopaSafestorageCxId = headerValues.getFirst(xPagopaSafestorageCxId);
                                   if ((apiKey != null && !apiKey.isEmpty()) &&
                                       (pagopaSafestorageCxId != null && !pagopaSafestorageCxId.isEmpty())) {
                                       return userConfigurationClientCall.getUser(pagopaSafestorageCxId)
                                                                         .onErrorResume(IdClientNotFoundException.class,
                                                                                        throwable -> Mono.error(new ResponseStatusException(
                                                                                                FORBIDDEN,
                                                                                                String.format("Invalid %s header",
                                                                                                              xPagopaSafestorageCxId))))
                                                                         .flatMap(userConfigurationResponse -> {
                                                                             if (userConfigurationResponse.getUserConfiguration()
                                                                                                          .getApiKey()
                                                                                                          .equals(apiKey)) {
                                                                                 return Mono.just(new KeyAuthenticationToken(apiKey,
                                                                                                                             pagopaSafestorageCxId));
                                                                             } else {
                                                                                 return Mono.error(new ResponseStatusException(FORBIDDEN,
                                                                                                                               String.format(
                                                                                                                                       "Invalid %s header",
                                                                                                                                       xApiKey)));
                                                                             }
                                                                         });
                                   } else {
                                       return Mono.empty();
                                   }
                               });
    }

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http,
                                                         ReactiveAuthenticationManager reactiveAuthenticationManager,
                                                         ServerAuthenticationConverter serverAuthenticationConverter) {

        final AuthenticationWebFilter authenticationWebFilter = new AuthenticationWebFilter(reactiveAuthenticationManager);
        authenticationWebFilter.setServerAuthenticationConverter(serverAuthenticationConverter);

        return http.authorizeExchange()
//                   .pathMatchers("/safe-storage/v1/**")
//                   .authenticated()
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
