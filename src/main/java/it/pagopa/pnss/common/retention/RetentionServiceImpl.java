package it.pagopa.pnss.common.retention;

import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import it.pagopa.pn.template.internal.rest.v1.dto.DocumentChanges;
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

import static it.pagopa.pnss.common.Constant.AVAILABLE;
import static it.pagopa.pnss.common.Constant.ATTACHED;

@Service
@Slf4j
public class RetentionServiceImpl extends CommonS3ObjectService implements RetentionService {
	
    @Value("${default.internal.x-api-key.value:#{null}}")
    private String defaultInteralApiKeyValue;

    @Value("${default.internal.header.x-pagopa-safestorage-cx-id:#{null}}")
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
					log.info("getRetentionPeriod() : configurationApiCall.getDocumentsConfigs() : call OK");
					
					DocumentTypeConfiguration dtcToRefer = null;
					for (DocumentTypeConfiguration dtc: response.getDocumentsTypes()) {
						log.info("getRetentionPeriod() : document type configuration '{}'", dtc.getName());
						if (dtc.getName().equalsIgnoreCase(documentType)) {
							dtcToRefer = dtc;
						}
					}
					log.info("getRetentionPeriod() : document type configuration ToRefer '{}'", dtcToRefer);
					if (dtcToRefer == null) {
						throw new RetentionException(
								String.format("DocumentTypeConfiguration not found for Document Type '%s' not found for Key '%s'", 
										documentType, documentKey));
					}
					
					for (StorageConfiguration sc: response.getStorageConfigurations()) {
						log.info("getRetentionPeriod() : storage configuration '{}'", sc.getName());
						if (sc.getName().equals(dtcToRefer.getStatuses().get(documentState).getStorage())) {
							log.info("getRetentionPeriod() : storage configuration ToRefer '{}'", sc.getName());
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
			log.info("getRetainUntilDate(): retaintUntilDate {}", retaintUntilDate);
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
		
		// se manca anche solo un elemento di autenticazione, imposto le credenziali con "utente interno"
		if (authPagopaSafestorageCxId == null || authPagopaSafestorageCxId.isBlank()
				|| authApiKey == null || authApiKey.isBlank()) {
			log.info("getRetentionUntil() : almeno uno tra authPagopaSafestorageCxId e authApiKey non e' valorizzato, utilizzo le credenziali interne");
			authPagopaSafestorageCxId = defaultInternalClientIdValue;
			authApiKey = defaultInteralApiKeyValue;
		}
		
		return getRetentionPeriodInDays(documentKey, documentState, documentType, authPagopaSafestorageCxId, authApiKey)
						.map(retentionPeriodInDays -> getRetainUntilDate(dataCreazioneObjectForBucket, retentionPeriodInDays));
	}
	
	@Override
	public Mono<DocumentEntity> setRetentionPeriodInBucketObjectMetadata(
			String authPagopaSafestorageCxId, String authApiKey, 
			DocumentChanges documentChanges, DocumentEntity documentEntity) {
		
		log.info("setRetentionPeriodInBucketObjectMetadata() : START : authPagopaSafestorageCxId {} : authApiKey {} : "
				+ "documentChanges {} : documentEntity {} ", 
				authPagopaSafestorageCxId, authApiKey,
				documentChanges, documentEntity);
		log.info("setRetentionPeriodInBucketObjectMetadata() : INPUT : retentionUntil = {} : ", 
				documentChanges != null && documentChanges.getRetentionUntil() == null ?
						"valore puntatore null" :
						(documentChanges != null && documentChanges.getRetentionUntil() != null && documentChanges.getRetentionUntil() .isBlank() ?
								"valore stringa vuota" :
								"(altro valore = <"+documentChanges.getRetentionUntil()+">)"));
		
		return  Mono.just(HeadObjectRequest.builder()
										   .bucket(bucketName.ssHotName())
										   .key(documentEntity.getDocumentKey())
										   .build())
					.flatMap(headObjectRequest -> Mono.fromCompletionStage(getS3AsynchClient().headObject(headObjectRequest)))
					.flatMap(headOjectResponse -> {
						log.info("setRetentionPeriodInBucketObjectMetadata() : "
								+ "headOjectResponse.lastModified() = {} :"
								+ "headOjectResponse.objectLockRetainUntilDate() = {} :"
								+ "headOjectResponse.objectLockModeAsString() = {} ", 
								headOjectResponse.lastModified(), 
								headOjectResponse.objectLockRetainUntilDate(),
								headOjectResponse.objectLockModeAsString());
						
						// VERIFICO LE CONDIZIONI preliminare all'impostazione della retentionUntil:
						
						// l'applicazione esterna impone la modifica della retentionUntil
						if (documentChanges.getRetentionUntil() != null
								&& !documentChanges.getRetentionUntil().isBlank()
								&& !documentChanges.getRetentionUntil().equalsIgnoreCase("null")) 
						{
							return 	Mono.just(documentChanges.getRetentionUntil())
										.flatMap(stringRetentionUntil -> {
											log.info("setRetentionPeriodInBucketObjectMetadata() : caso di rententionUtil specificata da applicazione chiamante :"
													+ " documentChanges.getRetentionUntil() = {}",
													documentChanges.getRetentionUntil());
											log.info("setRetentionPeriodInBucketObjectMetadata() : START formatting retentionUntil");
											final String PATTERN_FORMAT = "yyyy-MM-dd'T'HH:mm:ssXXX";
											DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(PATTERN_FORMAT,Locale.getDefault()); 
											LocalDateTime localDateTime = LocalDateTime.parse(stringRetentionUntil, dateTimeFormatter);
											ZonedDateTime zonedDateTime = localDateTime.atZone(ZoneId.systemDefault()); 
											Instant instantRetentionUntil = zonedDateTime.toInstant();
											log.info("setRetentionPeriodInBucketObjectMetadata() : END formatting retentionUntil");
											log.info("setRetentionPeriodInBucketObjectMetadata() : uso il seguente valore per impostare la retentionUntil per l'object:"
													+ " instantRetentionUntil = {}",
													instantRetentionUntil);
											return Mono.just(ObjectLockRetention.builder()
																				.retainUntilDate(instantRetentionUntil)
																				// riutilizzo il valore presente
																				.mode(headOjectResponse.objectLockModeAsString())
																				.build());
										})
										.flatMap(objectLockRetention -> Mono.just(PutObjectRetentionRequest.builder()
												 .bucket(bucketName.ssHotName())
												 .key(documentEntity.getDocumentKey())
												 .retention(objectLockRetention)
												 .build()))
										.flatMap(putObjectRetentionRequest -> Mono.fromCompletionStage(getS3AsynchClient().putObjectRetention(putObjectRetentionRequest)))
										.onErrorResume(RuntimeException.class, throwable -> {
											log.error("setRetentionPeriodInBucketObjectMetadata() : PutObjectRetentionResponse : "
													+ "errore generico verificatosi dopo la modifica della rententionUntil su oggetto nel bucket (documentChanges.getRetentionUntil() != null): "
													+ "documentKey {} : errore {}", 
											documentEntity.getDocumentKey(), throwable.getMessage(), throwable);
											return Mono.error(new RetentionException(throwable.getMessage()));
										})
										.thenReturn(documentEntity);
						}
						else if (
								// 1. l'oggetto nel bucket non ha una retaintUntilDate impostata
								//headOjectResponse.objectLockRetainUntilDate() == null 
								// 1. non si puo' distinguere il caso dell'impostazione di default da quella imposta dall'esterno o altro
								//    Fare riferimento alla entity, piuttosto che all'oggetto presente nel bucket
								documentEntity.getRetentionUntil() == null 
						// 2. l'applicazione esterna NON sta chiedendo di modificare la retentionUntil
								&& documentChanges.getRetentionUntil() == null
						// 3. lo stato dell'oggetto Document e' 'available'
								&& documentChanges.getDocumentState() != null
								&& (documentChanges.getDocumentState().equalsIgnoreCase(AVAILABLE)
										|| documentChanges.getDocumentState().equalsIgnoreCase(ATTACHED))) 
						{
							log.info("setRetentionPeriodInBucketObjectMetadata() : caso di rententionUtil NON impostata per l'object, NON specificata da applicazione chiamante E "
									+ " stato document = {}", documentChanges.getDocumentState());
							Instant dataCreazioneObjectInBucket = headOjectResponse.lastModified();
							log.info("setRetentionPeriodInBucketObjectMetadata() : dataCreazioneObjectInBucket = {}", dataCreazioneObjectInBucket);
							return getRetentionUntil(
											authPagopaSafestorageCxId, 
											authApiKey, 
											documentEntity.getDocumentKey(),
											// si assume che la postDocument abbia gia' effettuato la traduzione dello stato logico
											// e l'impostazione dello stesso stato nella entity
											documentEntity.getDocumentLogicalState(), // stato logico
											documentEntity.getDocumentType().getTipoDocumento(), 
											dataCreazioneObjectInBucket)
									.flatMap(istantRetentionUntil -> {
										log.info("setRetentionPeriodInBucketObjectMetadata() : START formatting retentionUntil");
										final String PATTERN_FORMAT = "yyyy-MM-dd'T'HH:mm:ssXXX";
										DateTimeFormatter formatter = DateTimeFormatter.ofPattern(PATTERN_FORMAT).withZone(ZoneId.systemDefault());
										// aggiorno la retentionUntilDate nella entity
										documentEntity.setRetentionUntil(formatter.format(istantRetentionUntil));
										log.info("setRetentionPeriodInBucketObjectMetadata() : END formatting retentionUntil");
										// restituisco l'objectLockRetention per il successivo aggiornamento del metadato per l'object nel bucket
										return Mono.just(ObjectLockRetention.builder()
																			.retainUntilDate(istantRetentionUntil)
																			// riutilizzo il valore presente
																			.mode(headOjectResponse.objectLockModeAsString())
																			.build());
									})
									.flatMap(objectLockRetention -> Mono.just(PutObjectRetentionRequest.builder()
																								 .bucket(bucketName.ssHotName())
																								 .key(documentEntity.getDocumentKey())
																								 .retention(objectLockRetention)
																								 .build()))
									.flatMap(putObjectRetentionRequest -> Mono.fromCompletionStage(getS3AsynchClient().putObjectRetention(putObjectRetentionRequest)))
									.onErrorResume(RuntimeException.class, throwable -> {
										log.error("setRetentionPeriodInBucketObjectMetadata() : PutObjectRetentionResponse : errore generico verificatosi dopo la modifica rentention su oggetto nel bucket : "
												+ "documentKey {} : errore {}", 
												documentEntity.getDocumentKey(), throwable.getMessage(), throwable);
										return Mono.error(new RetentionException(throwable.getMessage()));
									})
									.thenReturn(documentEntity);

						}
						log.info("setRetentionPeriodInBucketObjectMetadata() : don't need to access to object metadata (bucket)");
						return Mono.just(documentEntity);
					})
					.switchIfEmpty(Mono.error(new RetentionException(String.format("Object (in bucket) Data Creation not present (documentKey: %s)", documentEntity.getDocumentKey()))))
					
					// gestione errore
					.onErrorResume(NoSuchKeyException.class, throwable -> {
						log.error("setRetentionPeriodInBucketObjectMetadata() : documentKey = {} : errore = {}", 
								documentEntity.getDocumentKey(), throwable.getMessage(), throwable);
						return Mono.error(new RetentionException(throwable.getMessage()));
					})
					.onErrorResume(DateTimeException.class, throwable -> {
						log.error("setRetentionPeriodInBucketObjectMetadata() : documentKey = {} : errore formattazione instant retention util = {}", 
								documentEntity.getDocumentKey(), throwable.getMessage(), throwable);
						return Mono.error(new RetentionException(throwable.getMessage()));
					}
					)
					.onErrorResume(RuntimeException.class, throwable -> {
						log.error("setRetentionPeriodInBucketObjectMetadata() : errore generico = {}", throwable.getMessage(), throwable);
						return Mono.error(new RetentionException(throwable.getMessage()));
					});
	}

}
