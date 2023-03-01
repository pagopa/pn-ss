package it.pagopa.pnss.common.retention;

import java.time.Instant;
import java.time.Period;
import java.util.Map;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import it.pagopa.pn.template.rest.v1.dto.DocumentTypeConfiguration;
import it.pagopa.pn.template.rest.v1.dto.StorageConfiguration;
import it.pagopa.pnss.common.client.ConfigurationApiCall;
import it.pagopa.pnss.common.client.exception.RetentionException;
import it.pagopa.pnss.configurationproperties.BucketName;
import it.pagopa.pnss.repositorymanager.entity.DocumentEntity;
import it.pagopa.pnss.transformation.service.CommonS3ObjectService;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.ObjectLockRetention;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRetentionRequest;

@Service
@Slf4j
public class RetentionServiceImpl extends CommonS3ObjectService implements RetentionService {
	
    @Value("${default.internal.x-api-key.value}")
    private String defaultInteralApiKeyValue;

    @Value("${default.internal.header.x-pagopa-safestorage-cx-id}")
    private String defaultInternalClientIdValue;
	
    /*
     * In compliance mode, a protected object version can't be overwritten or deleted by any user, 
     * including the root user in your AWS account. 
     * When an object is locked in compliance mode, its retention mode can't be changed, 
     * and its retention period can't be shortened. 
     * Compliance mode helps ensure that an object version can't be overwritten or deleted for the duration of the retention period.
     */
	@Value("${object.lock.retention.mode}")
	private String objectLockRetentionMode;
	
	private final ConfigurationApiCall configurationApiCall;
	
	private final BucketName bucketName;

	public RetentionServiceImpl(ConfigurationApiCall configurationApiCall, BucketName bucketName) {
		this.configurationApiCall = configurationApiCall;
		this.bucketName = bucketName;
	}
	
	/*
	 * Controlla: StorageConfigurationsImpl.formatInYearsDays()
	 */
	private Integer getRetentionPeriodInDays(String retentionPeriod) throws RetentionException {
		log.info("getRetentionPeriodInDays() : START : retentionPeriod '{}'", retentionPeriod);
		
		if (retentionPeriod == null || retentionPeriod.isBlank() || retentionPeriod.length() < 2) {
			throw new RetentionException("Storage Configuration : Retention Period not found");
		}
		log.info("getRetentionPeriodInDays() : retentionPeriod '{}'", retentionPeriod);
		
		final String dayRef = "d";
		final String yearRef = "y";
		
		try {
			Integer yearsValue = retentionPeriod.contains(yearRef) ? 
					Integer.valueOf(retentionPeriod.substring(0, retentionPeriod.indexOf(yearRef)-1)) : 
					0;
			Integer daysValue = 0;
			log.info("getRetentionPeriodInDays() : yearsValue {} : daysValue {}", yearsValue, daysValue);
			if (yearsValue > 0) {
				daysValue = retentionPeriod.contains(dayRef) ? 
						Integer.valueOf(
								retentionPeriod.substring(
										retentionPeriod.indexOf(dayRef)+1, 
										retentionPeriod.length()
								).trim()) : 
								0;
			}
			else {
				daysValue = retentionPeriod.contains(dayRef) ? 
						Integer.valueOf(retentionPeriod.substring(0, retentionPeriod.indexOf(dayRef))) : 
						0;
			}
			log.info("getRetentionPeriodInDays() : retentionPeriod '{}' : daysValue {} - before : yearsValue {} - before", 
					retentionPeriod, daysValue, yearsValue);
			
			Integer days = (yearsValue > 0) ? 
						yearsValue * 365 + daysValue :
						daysValue;
			log.info("getRetentionPeriodInDays() : retentionPeriod '{}' : days {} - after", 
					retentionPeriod, days);
			return days;
		}
		catch (Exception e) {
			log.error("getRetentionPeriodInDays() : errore di conversione",e);
			throw new RetentionException("Can't convert retention period");
		}
	}
	
	private Mono<Integer> getRetentionPeriodInDays(
			String documentKey, String documentState, String documentType,
			String authPagopaSafestorageCxId, String authApiKey) throws RetentionException {
		log.info("getRetentionPeriod() : START : documentKey '{}' : documentState '{}' : documentType '{}'",
				documentKey, documentState, documentType);
        
		if (documentState == null || documentState.isBlank()) {
			throw new RetentionException(String.format(
					"Document State not present for Key '%s'", 
					documentKey));
		}
		if (documentType == null || documentType.isBlank()) {
			throw new RetentionException(String.format(
					"Document Type not present for Key '%s'", 
					documentKey));
		}
		
		return configurationApiCall.getDocumentsConfigs(authPagopaSafestorageCxId,authApiKey)
			.map(response -> {
					log.debug("getRetentionPeriod() : configurationApiCall.getDocumentsConfigs() : call OK");
					
					DocumentTypeConfiguration dtcToRefer = null;
					for (DocumentTypeConfiguration dtc: response.getDocumentsTypes()) {
						log.debug("getRetentionPeriod() : document type configuration '{}'", dtc.getName());
						if (dtc.getName().equalsIgnoreCase(documentType)) {
							dtcToRefer = dtc;
						}
					}
					log.debug("getRetentionPeriod() : document type configuration ToRefer '{}'", dtcToRefer);
					if (dtcToRefer == null) {
						throw new RetentionException(
								String.format("DocumentTypeConfiguration not found for Document Type '%s' not found for Key '%s'", 
										documentType, documentKey));
					}
					
					for (StorageConfiguration sc: response.getStorageConfigurations()) {
						log.debug("getRetentionPeriod() : storage configuration '{}'", sc.getName());
						if (sc.getName().equals(dtcToRefer.getStatuses().get(documentState).getStorage())) {
							log.debug("getRetentionPeriod() : storage configuration ToRefer '{}'", sc.getName());
							return getRetentionPeriodInDays(sc.getRetentionPeriod());
						}
					}
					throw new RetentionException(
							String.format("Storage Configuration not found for Key '%s'", 
							documentKey));
			})
			.onErrorResume(RuntimeException.class, e -> {
				log.error("getDefaultRetention() : errore : {}", e.getMessage(), e);
				throw new RetentionException(e.getMessage());
			});

	}
	
	@Deprecated(since="2.0")
	// la data di scadenza deve essere calcolata a partire dalla data di creazione dell'oggetto (da collocare nel bucket)
	private Instant getRetainUntilDate(Integer retentionPeriod) throws RetentionException {
		try {
			Instant retaintUntilDate = Instant.now().plus(Period.ofDays(retentionPeriod));
			log.debug("getRetainUntilDate(): retaintUntilDate {}", retaintUntilDate);
			return retaintUntilDate;
		}
		catch (Exception e) {
			log.error("getRetainUntilDate() : errore", e);
			throw new RetentionException(String.format("Error in Retain Until Date: %s", e.getMessage()));
		}
	}
	
	private Instant getRetainUntilDate(Instant dataCreazione, Integer retentionPeriod) throws RetentionException {
		try {
			return dataCreazione.plus(Period.ofDays(retentionPeriod));
		}
		catch (Exception e) {
			log.error("getRetainUntilDate() : errore", e);
			throw new RetentionException(String.format("Error in Retain Until Date: %s", e.getMessage()));
		}
	}
	

	// configurazione object lock direttamente per il bucket
//	@Override
//	public Mono<PutObjectLockConfigurationRequest> getPutObjectLockConfigurationRequest(String documentKey, 
//			String documentState, DocumentType documentType) throws RetentionException {
//		log.info("getPutObjectLockConfigurationRequest() : START : documentKey '{}' : documentState '{}' : documentType {}",
//				documentKey, documentState, (documentType == null ? "assente" : documentType.getTipoDocumento()));
//		
//		return getRetentionPeriod(documentKey, documentState, documentType.getTipoDocumento())
//				.map(retentionPeriod -> 
//					PutObjectLockConfigurationRequest.builder()
//					   .bucket(bucketName.ssHotName())
//					   .objectLockConfiguration(
//							   objLockConf -> objLockConf.objectLockEnabled(ObjectLockEnabled.ENABLED).rule(
//									   rule -> rule.defaultRetention(
//											   defaultRetention -> defaultRetention.days(retentionPeriod)
//											   									   .mode(objectLockRetentionMode))
//				        					)
//				        )
//					.build()
//				);
//	}
	
	@Override
	public Mono<PutObjectRequest> getPutObjectRequestForObjectInBucket(
			String bucketName, byte[] contentBytes, String documentKey, 
			String documentState, String documentType) throws RetentionException {
		log.info("getPutObjectRequestForObjectInBucket() : START : documentKey {} : documentState {} : documentType {}", 
				documentKey, documentState, documentType);
		
		return getRetentionPeriodInDays(documentKey, documentState, documentType, defaultInternalClientIdValue, defaultInteralApiKeyValue)
				.map(this::getRetainUntilDate)
				.map(retainUntilDate ->  PutObjectRequest.builder()
                        .bucket(bucketName)
                        .contentMD5(new String(Base64.encodeBase64(DigestUtils.md5(contentBytes))))
						.objectLockMode(objectLockRetentionMode)
						.objectLockRetainUntilDate(retainUntilDate)
                        .key(documentKey)
                        .build());
	}
	
	@Override
	public Mono<PutObjectRequest> getPutObjectRequestForPresignRequest(
			String bucketName, String documentKey, String contenType, Map<String,String> secret, 
			String documentState, String documentType,
			String authPagopaSafestorageCxId, String authApiKey) throws RetentionException {
		log.info("getPutObjectRequestForPresignRequest() : START : bucketName {} : keyName {} : contenType {} : secret {} : documentState {} : documentType {}", 
				bucketName, documentKey, contenType, secret,
				documentState, documentType);
		
		return getRetentionPeriodInDays(documentKey, documentState, documentType, authPagopaSafestorageCxId, authApiKey)
				.map(this::getRetainUntilDate)
				.map(retainUntilDate -> PutObjectRequest.builder()
							 .bucket(bucketName)
							 .key(documentKey)
							 .contentType(contenType)
							 .metadata(secret)
			                //.tagging(storageType)
							 .objectLockMode(objectLockRetentionMode)
							 .objectLockRetainUntilDate(retainUntilDate)
							 .build()
				);
	}
	
	@Override
	public Mono<Instant> getRetentionUntil(
			String authPagopaSafestorageCxId, String authApiKey,
			String documentKey, String documentState, String documentType,
			Instant dataCreazioneObjectForBucket) throws RetentionException {
		
		log.info("getRetentionUntil() : START : authPagopaSafestorageCxId {} : authApiKey {} : documentKey {} : documentState {} : documentType {}", 
				authPagopaSafestorageCxId, authApiKey,
				documentKey, documentState, documentType);
		
		return getRetentionPeriodInDays(documentKey, documentState, documentType, authPagopaSafestorageCxId, authApiKey)
						.map(retentionPeriodInDays -> getRetainUntilDate(dataCreazioneObjectForBucket, retentionPeriodInDays));
	}
	
	@Override
	public Mono<DocumentEntity> setRetentionPeriodInBucketObjectMetadata(
			String authPagopaSafestorageCxId, String authApiKey, 
			DocumentEntity documentEntity) {
		
		log.info("setRetentionPeriodInBucketObjectMetadata() : START : authPagopaSafestorageCxId {} : authApiKey {} : documentEntity {} ", 
				authPagopaSafestorageCxId, authApiKey,
				documentEntity);
		
		return  Mono.just(HeadObjectRequest.builder()
										   .bucket(bucketName.ssHotName())
										   .key(documentEntity.getDocumentKey())
										   .build())
					.flatMap(headObjectRequest -> Mono.fromCompletionStage(getS3AsynchClient().headObject(headObjectRequest)))
					.onErrorResume(NoSuchKeyException.class, throwable -> {
						log.error("setRetentionPeriodInBucketObjectMetadata() : AFTER headOjectReques: documentKey {} : errore: {}", 
								documentEntity.getDocumentKey(), throwable.getMessage(), throwable);
						return Mono.error(new RetentionException(throwable.getMessage()));
					})
					.onErrorResume(RuntimeException.class, throwable -> {
						log.error("setRetentionPeriodInBucketObjectMetadata() : AFTER headOjectRequest : errore generico : {}", 
								throwable.getMessage(), throwable);
						return Mono.error(new RetentionException(throwable.getMessage()));
					})
					.flatMap(headOjectResponse -> {
						log.info("patchDocument() : PRE recupero dataCreazioneObjectInBucket");
						Instant dataCreazioneObjectInBucket = null;
						if (headOjectResponse.objectLockRetainUntilDate() != null 
								&& documentEntity.getRetentionUntil() == null
								&& documentEntity.getDocumentState() != null
								&& documentEntity.getDocumentState().equalsIgnoreCase("available")) {
							dataCreazioneObjectInBucket = headOjectResponse.lastModified();
						}
						log.info("patchDocument() : POST recupero dataCreazioneObjectInBucket {}", dataCreazioneObjectInBucket);
						return Mono.just(dataCreazioneObjectInBucket);
					})
					.switchIfEmpty(Mono.error(new RetentionException(String.format("Object (in bucket) Data Creation not present (documentKey: %s)", documentEntity.getDocumentKey()))))
					.flatMap(dataCreazione -> getRetentionUntil(
														authPagopaSafestorageCxId, authApiKey, 
														documentEntity.getDocumentKey(), documentEntity.getDocumentState(), 
														documentEntity.getDocumentType().getTipoDocumento(), dataCreazione)
												.flatMap(istantRetentionUntil -> Mono.just(ObjectLockRetention.builder().retainUntilDate(istantRetentionUntil).build()))
												.flatMap(objectLockRetention -> Mono.just(PutObjectRetentionRequest.builder()
																											 .bucket(bucketName.ssHotName())
																											 .key(documentEntity.getDocumentKey())
																											 .retention(objectLockRetention)
																											 .build()))
												.flatMap(putObjectRetentionRequest -> Mono.fromCompletionStage(getS3AsynchClient().putObjectRetention(putObjectRetentionRequest)))
												.onErrorResume(RuntimeException.class, throwable -> {
													log.error("setRetentionPeriodInBucketObjectMetadata() : PutObjectRetentionResponse : errore generico : {}", 
															throwable.getMessage(), throwable);
													return Mono.error(new RetentionException(throwable.getMessage()));
												})
												.thenReturn(documentEntity) 
					)
					.onErrorResume(NoSuchKeyException.class, throwable -> {
						log.error("setRetentionPeriodInBucketObjectMetadata() : documentKey {} : errore: {}", 
								documentEntity.getDocumentKey(), throwable.getMessage(), throwable);
						return Mono.error(new RetentionException(throwable.getMessage()));
					})
					.onErrorResume(RuntimeException.class, throwable -> {
						log.error("setRetentionPeriodInBucketObjectMetadata() : errore generico : {}", throwable.getMessage(), throwable);
						return Mono.error(new RetentionException(throwable.getMessage()));
					});
	}

}
