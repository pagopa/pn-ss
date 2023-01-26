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

import it.pagopa.pnss.repositoryManager.dto.DocumentInput;
import it.pagopa.pnss.repositoryManager.dto.DocumentOutput;
import it.pagopa.pnss.repositoryManager.service.DocumentService;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/documents")
public class DocumentInternalApiController {

	@Autowired
	private DocumentService documentService;

	@GetMapping(value = "/{checkSum}")
	public Mono<ResponseEntity<DocumentOutput>> getdocument(@PathVariable("checkSum") String checkSum) 
	{
		DocumentOutput documentOut = documentService.getDocument(checkSum);
		return Mono.just(ResponseEntity.ok().body(documentOut));
	}

	@PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public Mono<ResponseEntity<DocumentOutput>> postdocument(@Valid @RequestBody DocumentInput documentInput) 
	{
		DocumentOutput documentOut = documentService.postdocument(documentInput);
		return Mono.just(ResponseEntity.ok().body(documentOut));
	}

	@PutMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public Mono<ResponseEntity<DocumentOutput>> updatedocument(@Valid @RequestBody DocumentInput document) 
	{
		DocumentOutput documentOut = documentService.updatedocument(document);
		return Mono.just(ResponseEntity.ok().body(documentOut));
	}

	@DeleteMapping(path = "/{checkSum}", produces = MediaType.APPLICATION_JSON_VALUE)
	public Mono<ResponseEntity<DocumentOutput>> deletedocument(@PathVariable("checkSum") String checkSum) 
	{
		DocumentOutput documentOut = documentService.deletedocument(checkSum);
		return Mono.just(ResponseEntity.ok().body(documentOut));
	}

}
