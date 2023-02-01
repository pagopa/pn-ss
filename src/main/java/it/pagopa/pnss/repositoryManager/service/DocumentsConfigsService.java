package it.pagopa.pnss.repositoryManager.service;

import it.pagopa.pn.template.rest.v1.dto.DocumentTypesConfigurations;
import reactor.core.publisher.Mono;

public interface DocumentsConfigsService {
	
	Mono<DocumentTypesConfigurations> getAllDocumentType();

}
