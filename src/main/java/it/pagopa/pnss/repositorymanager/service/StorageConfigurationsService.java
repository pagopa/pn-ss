package it.pagopa.pnss.repositorymanager.service;

import java.util.List;

import it.pagopa.pnss.common.client.dto.LifecycleRuleDTO;
import reactor.core.publisher.Mono;

public interface StorageConfigurationsService {
	
	//TODO sistemare
	public Mono<List<LifecycleRuleDTO>> getLifecycleConfiguration();
	//public Flux<LifecycleRuleDTO> getLifecycleConfiguration();

}
