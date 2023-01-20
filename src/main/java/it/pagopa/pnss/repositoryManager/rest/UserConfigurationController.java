package it.pagopa.pnss.repositoryManager.rest;


import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
	
	@Autowired
	UserConfigurationOutput userConfigurationOutput;
	
		// @GetMapping(value="/validate/{name}")
		@GetMapping(path="/getuser/{name}")
		public Mono <ResponseEntity <UserConfigurationOutput>> getUser(@RequestParam("name") String name){
			
			userConfigurationOutput = userService.getUser(name);
			
			Mono<ResponseEntity<UserConfigurationOutput>> result = Mono.just(ResponseEntity.ok().body(userConfigurationOutput));
			return result;
		}
	
		@PostMapping(path = "/postuser")
		public Mono<ResponseEntity<UserConfigurationOutput>> postUser(@Valid @RequestBody UserConfigurationInput user) {

			userConfigurationOutput = userService.postUser(user);

			Mono<ResponseEntity<UserConfigurationOutput>> result = Mono.just(ResponseEntity.ok().body(userConfigurationOutput));
			return result;
		}
		
		@PutMapping(path = "/updateuser")
		public Mono<ResponseEntity<UserConfigurationOutput>> updateUser(@Valid @RequestBody UserConfigurationInput user){
			
			userConfigurationOutput = userService.updateUser(user);
			
			Mono<ResponseEntity<UserConfigurationOutput>> result = Mono.just(ResponseEntity.ok().body(userConfigurationOutput));
			return result;
		}
		
		@DeleteMapping(path = "/deleteuser/{name}")
		public Mono<ResponseEntity<UserConfigurationOutput>> deleteUser(@RequestParam String name) {
			userConfigurationOutput = userService.deleteUser(name);

			Mono<ResponseEntity<UserConfigurationOutput>> result = Mono.just(ResponseEntity.ok().body(userConfigurationOutput));
			return result;
		}
		
		
}