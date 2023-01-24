package it.pagopa.pnss.repositoryManager.rest;

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

import it.pagopa.pnss.repositoryManager.dto.DocTypesInput;
import it.pagopa.pnss.repositoryManager.dto.DocTypesOutput;
import it.pagopa.pnss.repositoryManager.service.DocTypesService;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/doc-type")
public class DocTypesController {
	
	@Autowired
	private DocTypesService docTypesService;

//	private final DocTypesService docTypesService;
//
//	public DocTypesController(DocTypesService docTypesService) {
//		this.docTypesService = docTypesService;
//	}

//	@Autowired
//	DocTypesOutput documentOut = new DocTypesOutput();

	@GetMapping(value = "/getdoctypes/{name}")
	public Mono<ResponseEntity<DocTypesOutput>> getdocTypes(@RequestParam("name") String name) {

		DocTypesOutput docTypeOut = docTypesService.getDocType(name);

		Mono<ResponseEntity<DocTypesOutput>> result = Mono.just(ResponseEntity.ok().body(docTypeOut));
		return result;
	}

	@PostMapping(path = "/postdoctypes")
	public Mono<ResponseEntity<DocTypesOutput>> postdocTypes(@RequestBody DocTypesInput docTypes) {

		DocTypesOutput docTypeOut = docTypesService.postDocTypes(docTypes);

		Mono<ResponseEntity<DocTypesOutput>> result = Mono.just(ResponseEntity.ok().body(docTypeOut));
		return result;
	}

	@PutMapping(path = "/updatedoctypes/{name}")
	public Mono<ResponseEntity<DocTypesOutput>> updatedocTypes(@RequestParam("name") DocTypesInput docTypes) {

		DocTypesOutput docTypeOut = docTypesService.updateDocTypes(docTypes);

		Mono<ResponseEntity<DocTypesOutput>> result = Mono.just(ResponseEntity.ok().body(docTypeOut));
		return result;
	}

	@DeleteMapping(path = "/deletedoctypes/{name}")
	public Mono<ResponseEntity<DocTypesOutput>> deletedocTypes(@RequestParam("name") String name) {

		DocTypesOutput docTypeOut = docTypesService.deleteDocTypes(name);

		Mono<ResponseEntity<DocTypesOutput>> result = Mono.just(ResponseEntity.ok().body(docTypeOut));
		return result;
	}

}
