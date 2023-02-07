package it.pagopa.pnss.repositoryManager.service;

import java.util.List;

import it.pagopa.pnss.common.client.dto.LifecycleRuleDTO;

public interface StorageConfigurationsService {
	
	public List<LifecycleRuleDTO> getLifecycleConfiguration();

}
