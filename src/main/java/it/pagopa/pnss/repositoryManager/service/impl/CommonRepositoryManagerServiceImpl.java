package it.pagopa.pnss.repositoryManager.service.impl;

import it.pagopa.pnss.common.client.exception.IdClientNotFoundException;
import it.pagopa.pnss.repositoryManager.entity.DocTypeEntity;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
public class CommonRepositoryManagerServiceImpl {
	
	Mono<DocTypeEntity> getErrorIdClientNotFoundException(String typeId) {
		log.error("getDocType() : docType with typeId \"{}\" not found", typeId);
		return Mono.error(new IdClientNotFoundException(typeId));
	}

}
