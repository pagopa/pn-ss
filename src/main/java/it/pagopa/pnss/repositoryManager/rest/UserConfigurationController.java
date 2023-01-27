package it.pagopa.pnss.repositoryManager.rest;

import javax.validation.Valid;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import it.pagopa.pnss.repositoryManager.dto.UserConfigurationInput;
import it.pagopa.pnss.repositoryManager.dto.UserConfigurationOutput;
import it.pagopa.pnss.repositoryManager.service.UserConfigurationService;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/user-configuration")
public class UserConfigurationController {

	private final UserConfigurationService userService;

	public UserConfigurationController(UserConfigurationService userService) {
		this.userService = userService;
	}

	// @GetMapping(value="/validate/{name}")
	@GetMapping(path = "/{name}")
	public Mono<ResponseEntity<UserConfigurationOutput>> getUser(@PathVariable String name) {

		UserConfigurationOutput userConfigurationOutput = userService.getUser(name);

		Mono<ResponseEntity<UserConfigurationOutput>> result = Mono
				.just(ResponseEntity.ok().body(userConfigurationOutput));
		return result;
	}

	@PostMapping()
	public Mono<ResponseEntity<UserConfigurationOutput>> postUser(@Valid @RequestBody UserConfigurationInput user) {

		UserConfigurationOutput userConfigurationOutput = userService.postUser(user);

		Mono<ResponseEntity<UserConfigurationOutput>> result = Mono
				.just(ResponseEntity.ok().body(userConfigurationOutput));
		return result;
	}

	@PutMapping
	public Mono<ResponseEntity<UserConfigurationOutput>> updateUser(@Valid @RequestBody UserConfigurationInput user) {

		UserConfigurationOutput userConfigurationOutput = userService.updateUser(user);

		Mono<ResponseEntity<UserConfigurationOutput>> result = Mono
				.just(ResponseEntity.ok().body(userConfigurationOutput));
		return result;
	}

	@DeleteMapping(path = "/{name}")
	public Mono<ResponseEntity<UserConfigurationOutput>> deleteUser(@PathVariable String name) {
		
		UserConfigurationOutput userConfigurationOutput = userService.deleteUser(name);

		Mono<ResponseEntity<UserConfigurationOutput>> result = Mono
				.just(ResponseEntity.ok().body(userConfigurationOutput));
		return result;
	}

}