package it.pagopa.pnss.repositoryManager.rest.internal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;

import it.pagopa.pn.template.internal.rest.v1.api.UserConfigurationInternalApi;
import it.pagopa.pn.template.internal.rest.v1.dto.Error;
import it.pagopa.pn.template.internal.rest.v1.dto.UserConfiguration;
import it.pagopa.pn.template.internal.rest.v1.dto.UserConfigurationResponse;
import it.pagopa.pnss.common.client.exception.IdClientNotFoundException;
import it.pagopa.pnss.repositoryManager.exception.ItemAlreadyPresent;
import it.pagopa.pnss.repositoryManager.exception.RepositoryManagerException;
import it.pagopa.pnss.repositoryManager.service.UserConfigurationService;
import reactor.core.publisher.Mono;

@RestController
public class UserConfigurationInternalApiController implements UserConfigurationInternalApi {
 
	@Autowired
	private UserConfigurationService userConfigurationService;
	
	private UserConfigurationResponse getResponse(UserConfiguration userConfiguration) {
		UserConfigurationResponse response = new UserConfigurationResponse();
		response.setUserConfiguration(userConfiguration);
		return response;
	}
	
	private  Mono<ResponseEntity<UserConfigurationResponse>> getResponse(String name, Throwable throwable) 
	{
		UserConfigurationResponse response = new UserConfigurationResponse();
		response.setError(new Error());

		if (throwable instanceof ItemAlreadyPresent) {
			response.getError().setDescription(name == null ? "UserConfiguration already present" : String.format("UserConfiguration with id %s already present", name));
			return Mono.just(ResponseEntity.status(HttpStatus.FORBIDDEN).body(response));
		}
		else if (throwable instanceof IdClientNotFoundException) {
			response.getError().setDescription(name == null ? "UserConfiguration not found" : String.format("UserConfiguration with id %s not found", name));
			return Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND).body(response));
		}
		else if (throwable instanceof RepositoryManagerException) {
			response.getError().setDescription("UserConfiguration has incorrect attribute" );
			return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response));			
		}
		response.getError().setDescription(throwable.getMessage());
		return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response));
	}

	@Override
    public Mono<ResponseEntity<UserConfigurationResponse>> getUserConfiguration(String name,  final ServerWebExchange exchange) {
        
		return userConfigurationService.getUserConfiguration(name)
				.map(userConfiguration -> ResponseEntity.ok(getResponse(userConfiguration)))
				.onErrorResume(throwable -> getResponse(name, throwable));

    }
    
	@Override
    public Mono<ResponseEntity<UserConfigurationResponse>> insertUserConfiguration(Mono<UserConfiguration> userConfiguration,  final ServerWebExchange exchange) {

		return userConfiguration.flatMap(request -> userConfigurationService.insertUserConfiguration(request))
				.map(userConfigurationOutput -> ResponseEntity.ok(getResponse(userConfigurationOutput)))
				.onErrorResume(throwable -> getResponse(null, throwable));

    }

	@Override
    public Mono<ResponseEntity<UserConfigurationResponse>> patchUserConfiguration(String name, Mono<UserConfiguration> userConfiguration,  final ServerWebExchange exchange) {

		return userConfiguration.flatMap(request -> userConfigurationService.patchUserConfiguration(name, request))
				.map(userConfigurationOutput -> ResponseEntity.ok(getResponse(userConfigurationOutput)))
				.onErrorResume(throwable -> getResponse(name, throwable));
    }

	@Override
    public Mono<ResponseEntity<UserConfigurationResponse>> deleteUserConfiguration(String name,  final ServerWebExchange exchange) {
       
		return userConfigurationService.deleteUserConfiguration(name)
				.map(userConfigurationOutput -> ResponseEntity.ok(getResponse(userConfigurationOutput)))
				.onErrorResume(throwable -> getResponse(name, throwable));

    }

}