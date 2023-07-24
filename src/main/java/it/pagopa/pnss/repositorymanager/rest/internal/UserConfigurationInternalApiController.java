package it.pagopa.pnss.repositorymanager.rest.internal;

import it.pagopa.pnss.common.constant.Constant;
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
		final String GET_USER_CONFIGURATION = "getUserConfiguration";

		log.info(Constant.STARTING_PROCESS_ON, GET_USER_CONFIGURATION, name);

		log.debug(Constant.INVOKING_METHOD, GET_USER_CONFIGURATION, name);
		return userConfigurationService.getUserConfiguration(name)
				.map(userConfiguration -> {
					log.info(Constant.ENDING_PROCESS_ON, GET_USER_CONFIGURATION, name);
					return ResponseEntity.ok(getResponse(userConfiguration));
				})
				.onErrorResume(throwable -> {
					log.info(Constant.ENDING_PROCESS_WITH_ERROR, GET_USER_CONFIGURATION, throwable, throwable.getMessage());
					return getResponse(name, throwable);
				});

	}

	@Override
	public Mono<ResponseEntity<UserConfigurationResponse>> insertUserConfiguration(
			Mono<UserConfiguration> userConfiguration, final ServerWebExchange exchange) {
		final String INSERT_USER_CONFIGURATION = "insertUserConfiguration";


		return userConfiguration.doOnNext(userConfig ->{
				log.info(Constant.STARTING_PROCESS_ON, INSERT_USER_CONFIGURATION, userConfig == null ? null : userConfig.getName());
				})
				.flatMap(request -> {
				log.debug(Constant.INVOKING_METHOD, INSERT_USER_CONFIGURATION, request);
				return userConfigurationService.insertUserConfiguration(request);
				})
				.map(userConfigurationOutput -> {
					log.info(Constant.ENDING_PROCESS_ON, INSERT_USER_CONFIGURATION, userConfigurationOutput.getName());
					return ResponseEntity.ok(getResponse(userConfigurationOutput));
				})
				.onErrorResume(throwable -> {
					log.info(Constant.ENDING_PROCESS_WITH_ERROR, INSERT_USER_CONFIGURATION, throwable, throwable.getMessage());
					return getResponse(null, throwable);
				});

	}

	@Override
	public Mono<ResponseEntity<UserConfigurationResponse>> patchUserConfiguration(String name,
			Mono<UserConfigurationChanges> userConfigurationChanges, final ServerWebExchange exchange) {
		final String PATCH_USER_CONFIGURATION = "patchUserConfiguration";

		return userConfigurationChanges.doOnNext(userConfig ->{
					log.info(Constant.STARTING_PROCESS_ON, PATCH_USER_CONFIGURATION, userConfig == null ? null : userConfig.getApiKey());
				})
				.flatMap(request -> {
					log.debug(Constant.INVOKING_METHOD + " - '{}'", PATCH_USER_CONFIGURATION, name, request);
					return userConfigurationService.patchUserConfiguration(name, request);
				})
				.map(userConfigurationOutput -> {
					log.info(Constant.ENDING_PROCESS_ON, PATCH_USER_CONFIGURATION, userConfigurationOutput.getApiKey());
					return ResponseEntity.ok(getResponse(userConfigurationOutput));
				})
				.onErrorResume(throwable -> {
					log.info(Constant.ENDING_PROCESS_WITH_ERROR, PATCH_USER_CONFIGURATION, throwable, throwable.getMessage());
					return getResponse(name, throwable);
				});
	}

	@Override
	public Mono<ResponseEntity<Void>> deleteUserConfiguration(String name, final ServerWebExchange exchange) {
		final String DELETE_USER_CONFIGURATION = "deleteUserConfiguration";

		log.info(Constant.STARTING_PROCESS_ON, DELETE_USER_CONFIGURATION, name);

		log.debug(Constant.INVOKING_METHOD, DELETE_USER_CONFIGURATION, name);
		return userConfigurationService.deleteUserConfiguration(name)
				.map(docType -> {
					log.info(Constant.ENDING_PROCESS_ON, DELETE_USER_CONFIGURATION, name);
					return ResponseEntity.noContent().<Void>build();
				}).onErrorResume(IdClientNotFoundException.class,
						throwable -> {
							log.info(Constant.ENDING_PROCESS_WITH_ERROR, DELETE_USER_CONFIGURATION, throwable, throwable.getMessage());
						return Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND,
									throwable.getMessage(), throwable.getCause()));
						});

	}

}