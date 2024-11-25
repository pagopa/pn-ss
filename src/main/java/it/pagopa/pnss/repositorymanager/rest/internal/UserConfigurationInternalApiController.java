package it.pagopa.pnss.repositorymanager.rest.internal;

import it.pagopa.pn.safestorage.generated.openapi.server.v1.api.UserConfigurationInternalApi;
import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.UserConfiguration;
import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.UserConfigurationChanges;
import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.UserConfigurationResponse;
import it.pagopa.pnss.common.utils.LogUtils;
import lombok.CustomLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;

import it.pagopa.pnss.common.client.exception.IdClientNotFoundException;
import it.pagopa.pnss.repositorymanager.exception.ItemAlreadyPresent;
import it.pagopa.pnss.repositorymanager.exception.RepositoryManagerException;
import it.pagopa.pnss.repositorymanager.service.UserConfigurationService;
import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.Error;
import reactor.core.publisher.Mono;

@RestController
@CustomLog
public class UserConfigurationInternalApiController implements UserConfigurationInternalApi {

	private final UserConfigurationService userConfigurationService;

	public UserConfigurationInternalApiController(UserConfigurationService userConfigurationService) {
		this.userConfigurationService = userConfigurationService;
	}

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
		final String GET_USER_CONFIGURATION = "getUserConfiguration";
		log.logStartingProcess(GET_USER_CONFIGURATION);

		return userConfigurationService.getUserConfiguration(name)
				.map(userConfiguration -> ResponseEntity.ok(getResponse(userConfiguration)))
				.doOnSuccess(result -> log.logEndingProcess(GET_USER_CONFIGURATION))
				.onErrorResume(throwable -> {
					log.logEndingProcess(GET_USER_CONFIGURATION, false, throwable.getMessage());
					return getResponse(name, throwable);
				});

	}

	@Override
	public Mono<ResponseEntity<UserConfigurationResponse>> insertUserConfiguration(
			Mono<UserConfiguration> userConfiguration, final ServerWebExchange exchange) {
		final String INSERT_USER_CONFIGURATION = "insertUserConfiguration";
		log.logStartingProcess(INSERT_USER_CONFIGURATION);

		return userConfiguration
				.flatMap(userConfigurationService::insertUserConfiguration)
				.map(userConfigurationOutput -> ResponseEntity.ok(getResponse(userConfigurationOutput)))
				.doOnSuccess(result -> log.logEndingProcess(INSERT_USER_CONFIGURATION))
				.onErrorResume(throwable -> {
					log.logEndingProcess(INSERT_USER_CONFIGURATION, false, throwable.getMessage());
					return getResponse(null, throwable);
				});

	}

	@Override
	public Mono<ResponseEntity<UserConfigurationResponse>> patchUserConfiguration(String name,
																				  Mono<UserConfigurationChanges> userConfigurationChanges, final ServerWebExchange exchange) {
		final String PATCH_USER_CONFIGURATION = "patchUserConfiguration";
        log.logStartingProcess(PATCH_USER_CONFIGURATION);
		return userConfigurationChanges
				.flatMap(request -> userConfigurationService.patchUserConfiguration(name, request))
				.map(userConfigurationOutput -> ResponseEntity.ok(getResponse(userConfigurationOutput)))
				.doOnSuccess(result -> log.logEndingProcess(PATCH_USER_CONFIGURATION))
				.onErrorResume(throwable -> {
					log.logEndingProcess(PATCH_USER_CONFIGURATION, false, throwable.getMessage());
					return getResponse(name, throwable);
				});
	}

	@Override
	public Mono<ResponseEntity<Void>> deleteUserConfiguration(String name, final ServerWebExchange exchange) {
		final String DELETE_USER_CONFIGURATION = "deleteUserConfiguration";
		log.logStartingProcess(DELETE_USER_CONFIGURATION);

		return userConfigurationService.deleteUserConfiguration(name)
				.map(docType -> ResponseEntity.noContent().<Void>build())
				.doOnSuccess(result -> log.logEndingProcess(DELETE_USER_CONFIGURATION))
				.onErrorResume(IdClientNotFoundException.class, throwable -> {
					log.logEndingProcess(DELETE_USER_CONFIGURATION, false, throwable.getMessage());
					return Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND,
							throwable.getMessage(), throwable.getCause()));
				});
	}

}