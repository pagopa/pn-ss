package it.pagopa.pnss.repositoryManager.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import it.pagopa.pn.template.rest.v1.api.CfgApi;
import it.pagopa.pnss.repositoryManager.dto.DocTypesInput;
import it.pagopa.pnss.repositoryManager.dto.DocTypesOutput;
import it.pagopa.pnss.repositoryManager.service.DocTypesService;
import reactor.core.publisher.Mono;


	public class DocTypesController implements CfgApi {

		@Autowired
		DocTypesService docTypeService;

			
			@GetMapping(value="/getdoctypes/{name}")
			public Mono <ResponseEntity <DocTypesOutput>> getdocTypes(@RequestParam("name") String name){
				
				DocTypesOutput docTypeOut = docTypeService.getDocType(name);
				
				Mono<ResponseEntity<DocTypesOutput>> result = Mono.just(ResponseEntity.ok().body(docTypeOut));
				return result;
			}
		

			@PostMapping(path = "/postdoctypes")
			public Mono<ResponseEntity<DocTypesOutput>> postdocTypes(@RequestBody DocTypesInput docTypes) {

				DocTypesOutput docTypeOut = docTypeService.postDocTypes(docTypes);

				Mono<ResponseEntity<DocTypesOutput>> result = Mono.just(ResponseEntity.ok().body(docTypeOut));
				return result;
			}
			
			@PutMapping(path = "/updatedoctypes/{name}")
			public Mono<ResponseEntity<DocTypesOutput>> updatedocTypes(@RequestParam("name") DocTypesInput docTypes){
				
				DocTypesOutput docTypeOut = docTypeService.updateDocTypes(docTypes);
				
				Mono<ResponseEntity<DocTypesOutput>> result = Mono.just(ResponseEntity.ok().body(docTypeOut));
				return result;
			}
			
			@DeleteMapping(path = "/deletedoctypes/{name}")
			public Mono<ResponseEntity<DocTypesOutput>> deletedocTypes(@RequestParam("name") DocTypesInput docTypes){
				
				DocTypesOutput docTypeOut = docTypeService.deleteDocTypes(docTypes);
				
				Mono<ResponseEntity<DocTypesOutput>> result = Mono.just(ResponseEntity.ok().body(docTypeOut));
				return result;
			}
		

	

}
