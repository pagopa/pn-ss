package it.pagopa.pnss.repositorymanager.service;

import it.pagopa.pn.template.rest.v1.dto.DocumentTypesConfigurations;
import reactor.core.publisher.Mono;

public interface DocumentsConfigsService {
	
	Mono<DocumentTypesConfigurations> getDocumentsConfigs();

}
