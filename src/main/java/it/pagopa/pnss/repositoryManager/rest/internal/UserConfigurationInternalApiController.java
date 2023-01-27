package it.pagopa.pnss.repositoryManager.rest.internal;

import static org.springframework.http.HttpStatus.OK;

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
	private UserConfigurationService userService;

	@Override
    public Mono<ResponseEntity<UserConfiguration>> getUserConfiguration(String userName,  final ServerWebExchange exchange) {
        
		UserConfiguration userConfiguration = userService.getUserConfiguration(userName);
    	return Mono.just(ResponseEntity.ok().body(userConfiguration));

    }
    
	@Override
    public Mono<ResponseEntity<UserConfiguration>> insertUserConfiguration(Mono<UserConfiguration> userConfiguration,  final ServerWebExchange exchange) {

    	return userConfiguration.map(request -> {
    		UserConfiguration userConfigurationInserted = userService.insertUserConfiguration(request);
    		return ResponseEntity.ok().body(userConfigurationInserted);
    	});

    }

	@Override
    public Mono<ResponseEntity<UserConfiguration>> patchUserConfiguration(String userName, Mono<UserConfiguration> userConfiguration,  final ServerWebExchange exchange) {

    	return userConfiguration.map(request -> {
    		UserConfiguration userConfigurationUpdated = userService.patchUserConfiguration(userName, request);
    		return ResponseEntity.ok().body(userConfigurationUpdated);
    	});
    }

	@Override
    public Mono<ResponseEntity<Void>> deleteUserConfiguration(String userName,  final ServerWebExchange exchange) {
       
		userService.deleteUserConfiguration(userName);
    	return Mono.just(new ResponseEntity<>(OK));	

    }

}