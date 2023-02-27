package it.pagopa.pnss.common.retention;

import java.time.Instant;
import java.time.Period;
import java.util.Map;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;



import it.pagopa.pn.template.rest.v1.dto.DocumentTypeConfiguration;
import it.pagopa.pn.template.rest.v1.dto.StorageConfiguration;
import it.pagopa.pnss.common.client.ConfigurationApiCall;
import it.pagopa.pnss.common.client.exception.RetentionException;
import it.pagopa.pnss.configurationproperties.BucketName;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Service
@Slf4j
public class RetentionServiceImpl implements RetentionService {
	
    /*
     * In compliance mode, a protected object version can't be overwritten or deleted by any user, 
     * including the root user in your AWS account. 
     * When an object is locked in compliance mode, its retention mode can't be changed, 
     * and its retention period can't be shortened. 
     * Compliance mode helps ensure that an object version can't be overwritten or deleted for the duration of the retention period.
     */
	@Value("${object.lock.retention.mode}")
	private String objectLockRetentionMode;
	
    @Autowired
    private BucketName bucketName;
	
	private final ConfigurationApiCall configurationApiCall;

	public RetentionServiceImpl(ConfigurationApiCall configurationApiCall) {
		this.configurationApiCall = configurationApiCall;
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
	
	private Mono<Integer> getRetentionPeriod(String documentKey, String documentState, String documentType) throws RetentionException {
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
		
		return configurationApiCall.getDocumentsConfigs()
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
	public Mono<PutObjectRequest> getPutObjectRequestForObjectInBucket(String bucketName, byte[] contentBytes, String documentKey, 
			String documentState, String documentType) throws RetentionException {
		log.info("getPutObjectRequestForObjectInBucket() : START : documentKey {} : documentState {} : documentType {}", 
				documentKey, documentState, documentType);
		
		return getRetentionPeriod(documentKey, documentState, documentType)
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
	public Mono<PutObjectRequest> getPutObjectRequestForPresignRequest(String bucketName, String keyName, String contenType, Map<String,String> secret, 
			String documentKey, String documentState, String documentType) throws RetentionException {
		log.info("getPutObjectRequestForPresignRequest() : START : bucketName {} : keyName {} : contenType {} : secret {} : documentKey {} : documentState {} : documentType {}", 
				bucketName, keyName, contenType, secret,
				documentKey, documentState, documentType);
		
		return getRetentionPeriod(documentKey, documentState, documentType)
				.map(this::getRetainUntilDate)
				.map(retainUntilDate -> PutObjectRequest.builder()
							 .bucket(bucketName)
							 .key(keyName)
							 .contentType(contenType)
							 .metadata(secret)
			                //.tagging(storageType)
							 .objectLockMode(objectLockRetentionMode)
							 .objectLockRetainUntilDate(retainUntilDate)
							 .build()
				);
	}

}
