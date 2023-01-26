package it.pagopa.pnss.repositoryManager.rest.internal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;

import it.pagopa.pn.template.internal.rest.v1.api.DocTypeInternalApi;
import it.pagopa.pn.template.internal.rest.v1.dto.DocumentType;
import it.pagopa.pnss.repositoryManager.service.DocTypesService;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/safe-storage/internal/v1/doctypes")
public class DocTypesInternalApiController implements DocTypeInternalApi {
	
	@Autowired
	private DocTypesService docTypesService;

//	@GetMapping(value = "/{checksum}", produces = MediaType.APPLICATION_JSON_VALUE)
//	public Mono<ResponseEntity<DocTypesOutput>> getdocTypes(@PathVariable("checksum") String checksum) 
//	{
//		DocTypesOutput docTypeOut = docTypesService.getDocType(checksum);
//		return Mono.just(ResponseEntity.ok().body(docTypeOut));
//	}

    public Mono<ResponseEntity<DocumentType>> getDocType(String typeId,  final ServerWebExchange exchange) {

    	DocumentType documentType= docTypesService.getDocType(typeId);
    	return Mono.just(ResponseEntity.ok().body(documentType));

    }
	
//	@PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
//	public Mono<ResponseEntity<DocTypesOutput>> postdocTypes(@Valid @RequestBody DocTypesInput docTypes) 
//	{
//		DocTypesOutput docTypeOut = docTypesService.postDocTypes(docTypes);
//		return Mono.just(ResponseEntity.ok().body(docTypeOut));
//	}

    public Mono<ResponseEntity<DocumentType>> insertDocType(Mono<DocumentType> documentType,  final ServerWebExchange exchange) {

//    	docTypesService.insertDocTypes(documentType.);
    	return null;

    }
    
//	@PutMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
//	public Mono<ResponseEntity<DocTypesOutput>> updatedocTypes(@Valid @RequestBody DocTypesInput docTypes) 
//	{
//		DocTypesOutput docTypeOut = docTypesService.updateDocTypes(docTypes);
//		return Mono.just(ResponseEntity.ok().body(docTypeOut));
//	}
    
    public Mono<ResponseEntity<DocumentType>> updateDocType(String typeId, Mono<DocumentType> documentType,  final ServerWebExchange exchange) {

    	return null;

    }

//	@DeleteMapping(path = "/{checksum}", produces = MediaType.APPLICATION_JSON_VALUE)
//	public Mono<ResponseEntity<DocTypesOutput>> deletedocTypes(@PathVariable("checksum") String checksum) 
//	{
//		DocTypesOutput docTypeOut = docTypesService.deleteDocTypes(checksum);
//		return Mono.just(ResponseEntity.ok().body(docTypeOut));
//	}
	
    public Mono<ResponseEntity<DocumentType>> deleteDocType(String typeId,  final ServerWebExchange exchange) {

    	return null;	

    }

}
