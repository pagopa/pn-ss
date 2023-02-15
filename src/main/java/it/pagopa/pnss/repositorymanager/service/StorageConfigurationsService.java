package it.pagopa.pnss.repositorymanager.service;

import it.pagopa.pnss.common.client.dto.LifecycleRuleDTO;
import reactor.core.publisher.Flux;

public interface StorageConfigurationsService {
	
	public Flux<LifecycleRuleDTO> getLifecycleConfiguration();

}
