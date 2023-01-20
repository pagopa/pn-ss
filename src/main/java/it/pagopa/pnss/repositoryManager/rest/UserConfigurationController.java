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

import it.pagopa.pnss.repositoryManager.dto.UserConfigurationOutput;
import it.pagopa.pnss.repositoryManager.dto.UserConfigurationInput;
import it.pagopa.pnss.repositoryManager.model.UserConfigurationEntity;
import it.pagopa.pnss.repositoryManager.service.UserConfigurationService;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/user-configuration")
	public class UserConfigurationController {
	
	@Autowired
	UserConfigurationService userHandler;
	
		// @GetMapping(value="/validate/{name}")
		@GetMapping(path="/getuser/{name}")
		public Mono <ResponseEntity <UserConfigurationOutput>> getUser(@RequestParam("name") String name){
			
			UserConfigurationOutput userResp = userHandler.getUser(name);
			
			Mono<ResponseEntity<UserConfigurationOutput>> result = Mono.just(ResponseEntity.ok().body(userResp));
			return result;
		}
	
		

		@PostMapping(path = "/postuser")
		public Mono<ResponseEntity<UserConfigurationOutput>> postUser(@Valid @RequestBody UserConfigurationInput user) {

			UserConfigurationOutput userResp = userHandler.postUser(user);

			Mono<ResponseEntity<UserConfigurationOutput>> result = Mono.just(ResponseEntity.ok().body(userResp));
			return result;
		}
		
		@PutMapping(path = "/updateuser")
		public Mono<ResponseEntity<UserConfigurationOutput>> updateUser(@Valid @RequestBody UserConfigurationInput user){
			
			UserConfigurationOutput userResp = userHandler.updateUser(user);
			
			Mono<ResponseEntity<UserConfigurationOutput>> result = Mono.just(ResponseEntity.ok().body(userResp));
			return result;
		}
		
		@DeleteMapping(path = "/deleteuser")
		public Mono<ResponseEntity<UserConfigurationOutput>> deleteUser(@RequestBody UserConfigurationInput user) {
			UserConfigurationOutput userResp = userHandler.deleteUser(user);

			Mono<ResponseEntity<UserConfigurationOutput>> result = Mono.just(ResponseEntity.ok().body(userResp));
			return result;
		}
		
		
}