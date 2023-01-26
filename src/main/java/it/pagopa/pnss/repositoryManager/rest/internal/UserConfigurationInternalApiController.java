package it.pagopa.pnss.repositoryManager.rest.internal;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
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
@RequestMapping("/userconfigurations")
public class UserConfigurationInternalApiController {

	@Autowired
	private UserConfigurationService userService;

	@GetMapping(path = "/{name}")
	public Mono<ResponseEntity<UserConfigurationOutput>> getUser(@PathVariable String name) 
	{
		UserConfigurationOutput userConfigurationOutput = userService.getUser(name);
		return Mono.just(ResponseEntity.ok().body(userConfigurationOutput));
	}

	@PostMapping()
	public Mono<ResponseEntity<UserConfigurationOutput>> postUser(@Valid @RequestBody UserConfigurationInput user) 
	{
		UserConfigurationOutput userConfigurationOutput = userService.postUser(user);
		return Mono.just(ResponseEntity.ok().body(userConfigurationOutput));
	}

	@PutMapping
	public Mono<ResponseEntity<UserConfigurationOutput>> updateUser(@Valid @RequestBody UserConfigurationInput user) 
	{
		UserConfigurationOutput userConfigurationOutput = userService.updateUser(user);
		return Mono.just(ResponseEntity.ok().body(userConfigurationOutput));
	}

	@DeleteMapping(path = "/{name}")
	public Mono<ResponseEntity<UserConfigurationOutput>> deleteUser(@PathVariable String name) 
	{
		UserConfigurationOutput userConfigurationOutput = userService.deleteUser(name);
		return Mono.just(ResponseEntity.ok().body(userConfigurationOutput));
	}

}