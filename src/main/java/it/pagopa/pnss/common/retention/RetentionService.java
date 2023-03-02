package it.pagopa.pnss.common.retention;

import java.time.Instant;
import java.util.Map;

import it.pagopa.pn.template.internal.rest.v1.dto.DocumentChanges;
import it.pagopa.pnss.common.client.exception.RetentionException;
import it.pagopa.pnss.repositorymanager.entity.DocumentEntity;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

public interface RetentionService {
	
	// configurazione object lock direttamente per il bucket
//	Mono<PutObjectLockConfigurationRequest> getPutObjectLockConfigurationRequest(String documentKey, String documentState, DocumentType documentType) throws RetentionException;
	
	Mono<PutObjectRequest> getPutObjectRequestForObjectInBucket(
			String bucketName, byte[] contentBytes, 
			String documentKey, String documentState, String documentType) throws RetentionException;
	
	Mono<PutObjectRequest> getPutObjectRequestForPresignRequest(
			String bucketName, String documentKey, String contenType, Map<String,String> secret, 
			String documentState, String documentType,
			String authPagopaSafestorageCxId, String authApiKey) throws RetentionException;
	
	Mono<Instant> getRetentionUntil(
			String authPagopaSafestorageCxId, String authApiKey,
			String documentKey, String documentState, String documentType,
			Instant dataCreazioneObjectForBucket) throws RetentionException;
	
	Mono<DocumentEntity> setRetentionPeriodInBucketObjectMetadata(
			String authPagopaSafestorageCxId, String authApiKey, 
			DocumentChanges documentChanges, DocumentEntity documentEntity);

}
