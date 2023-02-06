package it.pagopa.pnss.repositoryManager.rest.internal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;

import it.pagopa.pn.template.internal.rest.v1.api.UserConfigurationInternalApi;
import it.pagopa.pn.template.internal.rest.v1.dto.UserConfiguration;
import it.pagopa.pnss.common.client.exception.IdClientNotFoundException;
import it.pagopa.pnss.repositoryManager.exception.ItemAlreadyPresent;
import it.pagopa.pnss.repositoryManager.service.UserConfigurationService;
import reactor.core.publisher.Mono;

@RestController
public class UserConfigurationInternalApiController implements UserConfigurationInternalApi {
 
	@Autowired
	private UserConfigurationService userConfigurationService;

	@Override
    public Mono<ResponseEntity<UserConfiguration>> getUserConfiguration(String name,  final ServerWebExchange exchange) {
        
		return userConfigurationService.getUserConfiguration(name)
				.map(ResponseEntity::ok)
    			.onErrorResume(error -> {
    				if (error instanceof IdClientNotFoundException) {
    					return Mono.just(ResponseEntity.notFound().build());
    				}
    				return Mono.just(ResponseEntity.badRequest().build());
    			});

    }
    
	@Override
    public Mono<ResponseEntity<UserConfiguration>> insertUserConfiguration(Mono<UserConfiguration> userConfiguration,  final ServerWebExchange exchange) {

		return userConfiguration.flatMap(request -> userConfigurationService.insertUserConfiguration(request))
				.map(ResponseEntity::ok)
    			.onErrorResume(error -> {
    				if (error instanceof ItemAlreadyPresent) {
    					return Mono.just(ResponseEntity.status(HttpStatus.FORBIDDEN).build());
    				}
    				return Mono.just(ResponseEntity.badRequest().build());
    			});

    }

	@Override
    public Mono<ResponseEntity<UserConfiguration>> patchUserConfiguration(String name, Mono<UserConfiguration> userConfiguration,  final ServerWebExchange exchange) {

		return userConfiguration.flatMap(request -> userConfigurationService.patchUserConfiguration(name, request))
				.map(ResponseEntity::ok)
    			.onErrorResume(IdClientNotFoundException.class, exception -> Mono.just(ResponseEntity.badRequest().build()));
    }

	@Override
    public Mono<ResponseEntity<UserConfiguration>> deleteUserConfiguration(String name,  final ServerWebExchange exchange) {
       
		return userConfigurationService.deleteUserConfiguration(name)
				.map(ResponseEntity::ok)
    			.onErrorResume(error -> {
    				if (error instanceof ItemAlreadyPresent) {
    					return Mono.just(ResponseEntity.notFound().build());
    				}
    				return Mono.just(ResponseEntity.badRequest().build());
    			});

    }

}