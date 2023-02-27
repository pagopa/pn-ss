package it.pagopa.pnss.common.retention;

import java.util.Map;

import it.pagopa.pnss.common.client.exception.RetentionException;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

public interface RetentionService {
	
//	Mono<PutObjectLockConfigurationRequest> getPutObjectLockConfigurationRequest(String documentKey, String documentState, DocumentType documentType) throws RetentionException;
	
	Mono<PutObjectRequest> getPutObjectRequestForObjectInBucket(String bucketName, byte[] contentBytes, String documentKey, String documentState, String documentType) throws RetentionException;
	
	Mono<PutObjectRequest> getPutObjectRequestForPresignRequest(String bucketName, String keyName, String contenType, Map<String,String> secret, 
			String documentKey, String documentState, String documentType) throws RetentionException;

}
