package it.pagopa.pnss.repositoryManager.rest.internal.impl;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
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
@RequestMapping("/safe-storage/internal/v1/doctypes")
public class DocTypesInternalApiController {
	
	@Autowired
	private DocTypesService docTypesService;

	// typeId
	@GetMapping(value = "/{checksum}", produces = MediaType.APPLICATION_JSON_VALUE)
	public Mono<ResponseEntity<DocTypesOutput>> getdocTypes(@PathVariable("checksum") String checksum) 
	{
		DocTypesOutput docTypeOut = docTypesService.getDocType(checksum);
		return Mono.just(ResponseEntity.ok().body(docTypeOut));
	}

	@PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public Mono<ResponseEntity<DocTypesOutput>> postdocTypes(@Valid @RequestBody DocTypesInput docTypes) 
	{
		DocTypesOutput docTypeOut = docTypesService.postDocTypes(docTypes);
		return Mono.just(ResponseEntity.ok().body(docTypeOut));
	}

	@PutMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public Mono<ResponseEntity<DocTypesOutput>> updatedocTypes(@Valid @RequestBody DocTypesInput docTypes) 
	{
		DocTypesOutput docTypeOut = docTypesService.updateDocTypes(docTypes);
		return Mono.just(ResponseEntity.ok().body(docTypeOut));
	}

	@DeleteMapping(path = "/{checksum}", produces = MediaType.APPLICATION_JSON_VALUE)
	public Mono<ResponseEntity<DocTypesOutput>> deletedocTypes(@PathVariable("checksum") String checksum) 
	{
		DocTypesOutput docTypeOut = docTypesService.deleteDocTypes(checksum);
		return Mono.just(ResponseEntity.ok().body(docTypeOut));
	}

}
