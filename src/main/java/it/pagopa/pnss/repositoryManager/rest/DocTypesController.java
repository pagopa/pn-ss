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

import it.pagopa.pnss.repositoryManager.dto.DocTypesInput;
import it.pagopa.pnss.repositoryManager.dto.DocTypesOutput;
import it.pagopa.pnss.repositoryManager.service.DocTypesService;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/doc-type")
public class DocTypesController {

	private final DocTypesService docTypesService;

	public DocTypesController(DocTypesService docTypesService) {
		this.docTypesService = docTypesService;
	}

	@GetMapping(value = "/{checksum}", produces = MediaType.APPLICATION_JSON_VALUE)
	public Mono<ResponseEntity<DocTypesOutput>> getdocTypes(@PathVariable("checksum") String checksum) {

		DocTypesOutput docTypeOut = docTypesService.getDocType(checksum);

		Mono<ResponseEntity<DocTypesOutput>> result = Mono.just(ResponseEntity.ok().body(docTypeOut));
		return result;
	}

	@PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public Mono<ResponseEntity<DocTypesOutput>> postdocTypes(@Valid @RequestBody DocTypesInput docTypes) {

		DocTypesOutput docTypeOut = docTypesService.postDocTypes(docTypes);

		Mono<ResponseEntity<DocTypesOutput>> result = Mono.just(ResponseEntity.ok().body(docTypeOut));
		return result;
	}

	@PutMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public Mono<ResponseEntity<DocTypesOutput>> updatedocTypes(@Valid @RequestBody DocTypesInput docTypes) {

		DocTypesOutput docTypeOut = docTypesService.updateDocTypes(docTypes);

		Mono<ResponseEntity<DocTypesOutput>> result = Mono.just(ResponseEntity.ok().body(docTypeOut));
		return result;
	}

	@DeleteMapping(path = "/{checksum}", produces = MediaType.APPLICATION_JSON_VALUE)
	public Mono<ResponseEntity<DocTypesOutput>> deletedocTypes(@PathVariable("checksum") String checksum) {

		DocTypesOutput docTypeOut = docTypesService.deleteDocTypes(checksum);

		Mono<ResponseEntity<DocTypesOutput>> result = Mono.just(ResponseEntity.ok().body(docTypeOut));
		return result;
	}

}
