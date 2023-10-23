package it.pagopa.pnss.repositorymanager.service;

import java.util.List;

import it.pagopa.pnss.common.client.dto.LifecycleRuleDTO;
import reactor.core.publisher.Mono;

public interface StorageConfigurationsService {

	Mono<List<LifecycleRuleDTO>> getLifecycleConfiguration();

}
