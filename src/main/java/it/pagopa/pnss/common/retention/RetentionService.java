package it.pagopa.pnss.common.retention;

import java.util.Map;

import it.pagopa.pn.template.internal.rest.v1.dto.DocumentType;
import it.pagopa.pnss.common.client.exception.RetentionException;
import software.amazon.awssdk.services.s3.model.PutObjectLockConfigurationRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

public interface RetentionService {
	
	PutObjectLockConfigurationRequest getPutObjectLockConfigurationRequest(String documentKey, String documentState, DocumentType documentType) throws RetentionException;
	
	PutObjectRequest getPutObjectForPresignRequest(String bucketName, String keyName, String contenType, Map<String,String> secret, 
			String documentKey, String documentState, String documentType) throws RetentionException;

}
