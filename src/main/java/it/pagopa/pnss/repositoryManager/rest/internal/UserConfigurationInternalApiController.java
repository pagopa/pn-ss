package it.pagopa.pnss.repositoryManager.rest.internal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;

import it.pagopa.pn.template.internal.rest.v1.api.UserConfigurationInternalApi;
import it.pagopa.pn.template.internal.rest.v1.dto.UserConfiguration;
import it.pagopa.pnss.repositoryManager.service.UserConfigurationService;
import reactor.core.publisher.Mono;

@RestController
public class UserConfigurationInternalApiController implements UserConfigurationInternalApi {
 
	@Autowired
	private UserConfigurationService userConfigurationService;

	@Override
    public Mono<ResponseEntity<UserConfiguration>> getUserConfiguration(String name,  final ServerWebExchange exchange) {
        
		return userConfigurationService.getUserConfiguration(name).map(ResponseEntity::ok);

    }
    
	@Override
    public Mono<ResponseEntity<UserConfiguration>> insertUserConfiguration(Mono<UserConfiguration> userConfiguration,  final ServerWebExchange exchange) {

		return userConfiguration.flatMap(request -> userConfigurationService.insertUserConfiguration(request)).map(ResponseEntity::ok);

    }

	@Override
    public Mono<ResponseEntity<UserConfiguration>> patchUserConfiguration(String name, Mono<UserConfiguration> userConfiguration,  final ServerWebExchange exchange) {

		return userConfiguration.flatMap(request -> userConfigurationService.patchUserConfiguration(name, request)).map(ResponseEntity::ok);
    }

	@Override
    public Mono<ResponseEntity<UserConfiguration>> deleteUserConfiguration(String name,  final ServerWebExchange exchange) {
       
		return userConfigurationService.deleteUserConfiguration(name).map(ResponseEntity::ok);

    }

}