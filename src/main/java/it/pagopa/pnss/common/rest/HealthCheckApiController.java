package it.pagopa.pnss.common.rest;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Mono;

@RestController
public class HealthCheckApiController {

	@GetMapping(value = "/")
    public Mono<ResponseEntity<Void>> status() {
        return Mono.just(ResponseEntity.ok().build());
    }
}
