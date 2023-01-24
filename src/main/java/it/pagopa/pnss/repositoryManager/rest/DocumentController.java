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

import it.pagopa.pnss.repositoryManager.dto.DocumentInput;
import it.pagopa.pnss.repositoryManager.dto.DocumentOutput;
import it.pagopa.pnss.repositoryManager.service.DocumentService;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/document")
public class DocumentController {

//	private final DocumentService documentService;
//
//	public DocumentController(DocumentService documentService) {
//		this.documentService = documentService;
//	}
//
//	@Autowired
//	DocumentOutput documentOut = new DocumentOutput();
	
	@Autowired
	private DocumentService documentService;

	// @GetMapping(value="/validate/{name}")
	@GetMapping(value = "/document/{name}")
	public Mono<ResponseEntity<DocumentOutput>> getdocument(@RequestParam("name") String name) {

		DocumentOutput documentOut = documentService.getDocument(name);

		Mono<ResponseEntity<DocumentOutput>> result = Mono.just(ResponseEntity.ok().body(documentOut));
		return result;
	}

	@PostMapping(path = "/document")
	public Mono<ResponseEntity<DocumentOutput>> postdocument(@Valid @RequestBody DocumentInput documentInput) {

		DocumentOutput documentOut = documentService.postdocument(documentInput);

		Mono<ResponseEntity<DocumentOutput>> result = Mono.just(ResponseEntity.ok().body(documentOut));
		return result;
	}

	@PutMapping(path = "/document/{name}")
	public Mono<ResponseEntity<DocumentOutput>> updatedocument(@RequestBody DocumentInput document) {

		DocumentOutput documentOut = documentService.updatedocument(document);

		Mono<ResponseEntity<DocumentOutput>> result = Mono.just(ResponseEntity.ok().body(documentOut));
		return result;
	}

	@DeleteMapping(path = "/document/{name}")
	public Mono<ResponseEntity<DocumentOutput>> deletedocument(@RequestParam("name") String name) {

		DocumentOutput documentOut = documentService.deletedocument(name);

		Mono<ResponseEntity<DocumentOutput>> result = Mono.just(ResponseEntity.ok().body(documentOut));
		return result;
	}

}
