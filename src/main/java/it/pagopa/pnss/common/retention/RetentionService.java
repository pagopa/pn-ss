package it.pagopa.pnss.common.retention;

import java.time.Instant;

import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.DocumentChanges;
import it.pagopa.pnss.common.client.exception.RetentionException;
import it.pagopa.pnss.repositorymanager.entity.DocumentEntity;
import reactor.core.publisher.Mono;

public interface RetentionService {
	
	Mono<Instant> getRetentionUntil(
			String authPagopaSafestorageCxId, String authApiKey,
			String documentKey, String documentState, String documentType,
			Instant dataCreazioneObjectForBucket) throws RetentionException;
	
	Mono<DocumentEntity> setRetentionPeriodInBucketObjectMetadata(
			String authPagopaSafestorageCxId, String authApiKey,
			DocumentChanges documentChanges, DocumentEntity documentEntity,
			String oldState);

}
