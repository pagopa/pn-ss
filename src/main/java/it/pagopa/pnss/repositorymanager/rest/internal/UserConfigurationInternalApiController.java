package it.pagopa.pnss.repositorymanager.rest.internal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;

import it.pagopa.pn.template.internal.rest.v1.api.UserConfigurationInternalApi;
import it.pagopa.pn.template.internal.rest.v1.dto.Error;
import it.pagopa.pn.template.internal.rest.v1.dto.UserConfiguration;
import it.pagopa.pn.template.internal.rest.v1.dto.UserConfigurationChanges;
import it.pagopa.pn.template.internal.rest.v1.dto.UserConfigurationResponse;
import it.pagopa.pnss.common.client.exception.DocumentKeyNotPresentException;
import it.pagopa.pnss.common.client.exception.IdClientNotFoundException;
import it.pagopa.pnss.repositorymanager.exception.ItemAlreadyPresent;
import it.pagopa.pnss.repositorymanager.exception.RepositoryManagerException;
import it.pagopa.pnss.repositorymanager.service.UserConfigurationService;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@RestController
@Slf4j
public class UserConfigurationInternalApiController implements UserConfigurationInternalApi {

	@Autowired
	private UserConfigurationService userConfigurationService;

	private UserConfigurationResponse getResponse(UserConfiguration userConfiguration) {
		UserConfigurationResponse response = new UserConfigurationResponse();
		response.setUserConfiguration(userConfiguration);
		return response;
	}

	private Mono<ResponseEntity<UserConfigurationResponse>> buildErrorResponse(HttpStatus httpStatus, String errorMsg) {
		UserConfigurationResponse response = new UserConfigurationResponse();
		response.setError(new Error());
		response.getError().setDescription(errorMsg);
		return Mono.just(ResponseEntity.status(httpStatus).body(response));
	}

	private Mono<ResponseEntity<UserConfigurationResponse>> buildErrorResponse(HttpStatus httpStatus,
			Throwable throwable) {
		UserConfigurationResponse response = new UserConfigurationResponse();
		response.setError(new Error());
		response.getError().setDescription(throwable.getMessage());
		return Mono.just(ResponseEntity.status(httpStatus).body(response));
	}

	private Mono<ResponseEntity<UserConfigurationResponse>> getResponse(String name, Throwable throwable) {
		if (throwable instanceof ItemAlreadyPresent) {
			String errorMsg = name == null ? "UserConfiguration already present"
					: String.format("UserConfiguration with name %s already present", name);
			return buildErrorResponse(HttpStatus.CONFLICT, errorMsg);
		} else if (throwable instanceof IdClientNotFoundException) {
			String errorMsg = name == null ? "UserConfiguration not found"
					: String.format("UserConfiguration with name %s not found", name);
			return buildErrorResponse(HttpStatus.NOT_FOUND, errorMsg);
		} else if (throwable instanceof RepositoryManagerException) {
			return buildErrorResponse(HttpStatus.BAD_REQUEST, throwable);
		} else {
			log.error("Internal Error ---> {}", throwable.getMessage());
			return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, throwable);
		}
	}

	@Override
	public Mono<ResponseEntity<UserConfigurationResponse>> getUserConfiguration(String name,
			final ServerWebExchange exchange) {

		return userConfigurationService.getUserConfiguration(name)
				.map(userConfiguration -> ResponseEntity.ok(getResponse(userConfiguration)))
				.onErrorResume(throwable -> getResponse(name, throwable));

	}

	@Override
	public Mono<ResponseEntity<UserConfigurationResponse>> insertUserConfiguration(
			Mono<UserConfiguration> userConfiguration, final ServerWebExchange exchange) {

		return userConfiguration.flatMap(request -> userConfigurationService.insertUserConfiguration(request))
				.map(userConfigurationOutput -> ResponseEntity.ok(getResponse(userConfigurationOutput)))
				.onErrorResume(throwable -> getResponse(null, throwable));

	}

	@Override
	public Mono<ResponseEntity<UserConfigurationResponse>> patchUserConfiguration(String name,
			Mono<UserConfigurationChanges> userConfigurationChanges, final ServerWebExchange exchange) {

		return userConfigurationChanges
				.flatMap(request -> userConfigurationService.patchUserConfiguration(name, request))
				.map(userConfigurationOutput -> ResponseEntity.ok(getResponse(userConfigurationOutput)))
				.onErrorResume(throwable -> getResponse(name, throwable));
	}

	@Override
	public Mono<ResponseEntity<Void>> deleteUserConfiguration(String name, final ServerWebExchange exchange) {

		return userConfigurationService.deleteUserConfiguration(name)
				.map(docType -> ResponseEntity.noContent().<Void>build()).onErrorResume(IdClientNotFoundException.class,
						throwable -> Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND,
								throwable.getMessage(), throwable.getCause())));

	}

}