package it.pagopa.pnss.repositorymanager.service;

import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.DocumentTypesConfigurations;
import reactor.core.publisher.Mono;

public interface DocumentsConfigsService {
	
	Mono<DocumentTypesConfigurations> getDocumentsConfigs();

}
