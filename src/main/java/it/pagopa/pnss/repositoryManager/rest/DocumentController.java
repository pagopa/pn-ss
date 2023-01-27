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

import it.pagopa.pnss.repositoryManager.dto.DocumentInput;
import it.pagopa.pnss.repositoryManager.dto.DocumentOutput;
import it.pagopa.pnss.repositoryManager.service.DocumentService;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/document")
public class DocumentController {

	private final DocumentService documentService;

	public DocumentController(DocumentService documentService) {
		this.documentService = documentService;
	}

	// @GetMapping(value="/validate/{name}")
	@GetMapping(value = "/{checkSum}")
	public Mono<ResponseEntity<DocumentOutput>> getdocument(@PathVariable("checkSum") String checkSum) {

		DocumentOutput documentOut = documentService.getDocument(checkSum);

		Mono<ResponseEntity<DocumentOutput>> result = Mono.just(ResponseEntity.ok().body(documentOut));
		return result;
	}

	@PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public Mono<ResponseEntity<DocumentOutput>> postdocument(@Valid @RequestBody DocumentInput documentInput) {

		DocumentOutput documentOut = documentService.postdocument(documentInput);

		Mono<ResponseEntity<DocumentOutput>> result = Mono.just(ResponseEntity.ok().body(documentOut));
		return result;
	}

	@PutMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public Mono<ResponseEntity<DocumentOutput>> updatedocument(@Valid @RequestBody DocumentInput document) {

		DocumentOutput documentOut = documentService.updatedocument(document);

		Mono<ResponseEntity<DocumentOutput>> result = Mono.just(ResponseEntity.ok().body(documentOut));
		return result;
	}

	@DeleteMapping(path = "/{checkSum}", produces = MediaType.APPLICATION_JSON_VALUE)
	public Mono<ResponseEntity<DocumentOutput>> deletedocument(@PathVariable("checkSum") String checkSum) {

		DocumentOutput documentOut = documentService.deletedocument(checkSum);

		Mono<ResponseEntity<DocumentOutput>> result = Mono.just(ResponseEntity.ok().body(documentOut));
		return result;
	}

}
