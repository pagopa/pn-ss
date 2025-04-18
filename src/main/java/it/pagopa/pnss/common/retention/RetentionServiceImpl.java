package it.pagopa.pnss.common.retention;

import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.DocumentChanges;
import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.DocumentTypeConfiguration;
import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.StorageConfiguration;
import it.pagopa.pnss.common.client.ConfigurationApiCall;
import it.pagopa.pnss.common.client.exception.RetentionException;
import it.pagopa.pnss.common.client.exception.RetentionToIgnoreException;
import it.pagopa.pnss.common.utils.LogUtils;
import it.pagopa.pnss.configurationproperties.BucketName;
import it.pagopa.pnss.repositorymanager.entity.DocumentEntity;
import it.pagopa.pnss.transformation.service.S3Service;
import lombok.CustomLog;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.util.retry.RetryBackoffSpec;
import software.amazon.awssdk.services.s3.model.ObjectLockRetention;
import software.amazon.awssdk.services.s3.model.S3Exception;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.stream.Stream;

import static it.pagopa.pnss.common.constant.Constant.*;
import static it.pagopa.pnss.common.utils.LogUtils.*;

@Service
@CustomLog
public class RetentionServiceImpl implements RetentionService {

    private final S3Service s3Service;
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
    @Value("${retention.days.toIgnore}")
    private Integer retentionDaysToIgnore;
    private final ConfigurationApiCall configurationApiCall;
    private final BucketName bucketName;
    private static final String PATTERN_FORMAT = "yyyy-MM-dd'T'HH:mm:ssXXX";
    private final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern(PATTERN_FORMAT).withZone(ZoneId.systemDefault());
    private final RetryBackoffSpec gestoreRepositoryRetryStrategy;

    public RetentionServiceImpl(ConfigurationApiCall configurationApiCall, BucketName bucketName, RetryBackoffSpec gestoreRepositoryRetryStrategy,S3Service s3Service) {
        this.configurationApiCall = configurationApiCall;
        this.bucketName = bucketName;
        this.gestoreRepositoryRetryStrategy = gestoreRepositoryRetryStrategy;
        this.s3Service = s3Service;
    }

    /*
     * Controlla: StorageConfigurationsImpl.formatInYearsDays()
     */
    public Integer getRetentionPeriodInDays(String retentionPeriod) throws RetentionException {

        if (retentionPeriod == null || retentionPeriod.isBlank() || retentionPeriod.length() < 2) {
            throw new RetentionException("Storage Configuration : Retention Period not found");
        }

        final String dayRef = "d";
        final String yearRef = "y";

        String retentionPeriodStr =  retentionPeriod.replace(" ", "").toLowerCase();

        try {
            int yearsValue = retentionPeriodStr.contains(yearRef) ? Integer.parseInt(retentionPeriodStr.substring(0,
                    retentionPeriodStr.indexOf(yearRef))) : 0;
            int daysValue = 0;
            if (yearsValue > 0) {
                daysValue =
                        retentionPeriodStr.contains(dayRef) ? Integer.parseInt(retentionPeriodStr.substring(retentionPeriodStr.indexOf(yearRef) + 1,
                                retentionPeriodStr.indexOf(dayRef))) : 0;
            } else {
                daysValue = retentionPeriodStr.contains(dayRef) ? Integer.parseInt(retentionPeriodStr.substring(0,
                        retentionPeriodStr.indexOf(dayRef))) : 0;
            }

            return (yearsValue > 0) ? yearsValue * 365 + daysValue : daysValue;
        } catch (Exception e) {
            log.error("getRetentionPeriodInDays() : errore di conversione", e);
            throw new RetentionException("Can't convert retention period");
        }
    }

    protected Mono<Integer> getRetentionPeriodInDays(String documentKey, String documentState, String documentType,
                                                     String authPagopaSafestorageCxId, String authApiKey)
            throws RetentionException {

        validateInput(documentKey, documentState, documentType);

        return configurationApiCall.getDocumentsConfigs(authPagopaSafestorageCxId, authApiKey)
                .retryWhen(gestoreRepositoryRetryStrategy)
                .map(response -> {

                    DocumentTypeConfiguration dtcToRefer = null;
                    for (DocumentTypeConfiguration dtc : response.getDocumentsTypes()) {
                        if (dtc.getName().equalsIgnoreCase(documentType)) {
                            dtcToRefer = dtc;
                        }
                    }
                    if (dtcToRefer == null) {
                        throw new RetentionException(String.format(
                                "DocumentTypeConfiguration not found for Document Type '%s' not found for Key '%s'",
                                documentType,
                                documentKey));
                    }

                    for (StorageConfiguration sc : response.getStorageConfigurations()) {
                        if (sc.getName().equals(dtcToRefer.getStatuses().get(documentState).getStorage())) {
                            var retentionPeriod = sc.getRetentionPeriod();
                            return getRetentionPeriodInDays(retentionPeriod);
                        }
                    }
                    throw new RetentionException(String.format("Storage Configuration not found for Key '%s'", documentKey));
                }).doOnError(e -> log.error("getDefaultRetention() : errore : {}", e.getMessage(), e));

    }

    private static void validateInput(String documentKey, String documentState, String documentType) {
        if (documentState == null || documentState.isBlank()) {
            throw new RetentionException(String.format("Document State not present for Key '%s'", documentKey));
        }
        if (documentType == null || documentType.isBlank()) {
            throw new RetentionException(String.format("Document Type not present for Key '%s'", documentKey));
        }
    }

    protected Instant getRetainUntilDate(Instant dataCreazione, Integer retentionPeriod) throws RetentionException {
        try {
            return dataCreazione.plus(Period.ofDays(retentionPeriod));
        } catch (Exception e) {
            log.error("getRetainUntilDate() : errore", e);
            throw new RetentionException(String.format("Error in Retain Until Date: %s", e.getMessage()));
        }
    }

    @Override
    public Mono<Instant> getRetentionUntil(String authPagopaSafestorageCxId, String authApiKey, String documentKey, String documentState,
                                           String documentType, Instant dataCreazioneObjectForBucket)
            throws RetentionException {
        log.debug(INVOKING_METHOD, GET_RETENTION_UNTIL, Stream.of(documentKey, documentState, documentType, dataCreazioneObjectForBucket).toList());
        // se manca anche solo un elemento di autenticazione, imposto le credenziali con "utente interno"
        if (authPagopaSafestorageCxId == null || authPagopaSafestorageCxId.isBlank() || authApiKey == null || authApiKey.isBlank()) {
            log.debug("getRetentionUntil() : almeno uno tra authPagopaSafestorageCxId e authApiKey non e' valorizzato, utilizzo le " +
                     "credenziali interne");
            authPagopaSafestorageCxId = defaultInternalClientIdValue;
            authApiKey = defaultInteralApiKeyValue;
        }

        return getRetentionPeriodInDays(documentKey, documentState, documentType, authPagopaSafestorageCxId, authApiKey)
                .handle((retentionPeriodInDays, sink) ->
                {
                    if (Objects.equals(retentionPeriodInDays, retentionDaysToIgnore))
                        sink.error(new RetentionToIgnoreException());
                    else sink.next(retentionPeriodInDays);
                })
                .cast(Integer.class)
                .map(retentionPeriodInDays -> getRetainUntilDate(dataCreazioneObjectForBucket, retentionPeriodInDays))
                .doOnSuccess(result -> log.info(SUCCESSFUL_OPERATION_LABEL, GET_RETENTION_UNTIL, result));
    }

    @Override
    public Mono<DocumentEntity> setRetentionPeriodInBucketObjectMetadata(String authPagopaSafestorageCxId, String authApiKey,
                                                                         DocumentChanges documentChanges, DocumentEntity documentEntity,
                                                                         String oldState) {
        log.debug(INVOKING_METHOD, SET_RETENTION_PERIOD_IN_BUCKET_OBJECT_METADATA, Stream.of(authPagopaSafestorageCxId, documentChanges, documentEntity, oldState).toList());
                  return s3Service.headObject(documentEntity.getDocumentKey(), bucketName.ssHotName())
                   .flatMap(headObjectResponse -> {
                       log.debug("setRetentionPeriodInBucketObjectMetadata() : " + "headOjectResponse.lastModified() = {} :" +
                                "headOjectResponse.objectLockRetainUntilDate() = {} :" + "objectLockRetentionMode = {} ",
                                headObjectResponse.lastModified(),
                                headObjectResponse.objectLockRetainUntilDate(),
                                objectLockRetentionMode);

                       if (objectLockRetentionMode == null || objectLockRetentionMode.isBlank()) {
                           return Mono.error(new RetentionException("Valore non trovato per la variabile \"PnSsBucketLockRetentionMode\""));
                       }

                       // VERIFICO LE CONDIZIONI preliminare all'impostazione della retentionUntil:

                       // l'applicazione esterna impone la modifica della retentionUntil
                       if (documentChanges.getRetentionUntil() != null && !documentChanges.getRetentionUntil().isBlank() &&
                           !documentChanges.getRetentionUntil().equalsIgnoreCase("null")) {

                           Instant parsedRetentionUntil = Instant.parse(documentChanges.getRetentionUntil());

                           if (headObjectResponse.objectLockRetainUntilDate().truncatedTo(ChronoUnit.SECONDS).equals(parsedRetentionUntil))
                               return Mono.just(documentEntity);

                           return Mono.just(ObjectLockRetention.builder()
                                           .retainUntilDate(parsedRetentionUntil)
                                           .mode(objectLockRetentionMode)
                                           .build())
                                     .flatMap(objectLockRetention -> s3Service.putObjectRetention(documentEntity.getDocumentKey(), bucketName.ssHotName(), objectLockRetention)
                                                .doOnSuccess(result -> documentEntity.setRetentionUntil(documentChanges.getRetentionUntil()))
                                                .onErrorResume(S3Exception.class, throwable -> {
                                                    String errMsg = String.format("Error updating retention date '%s' from S3 bucket '%s' on document '%s'", objectLockRetention.retainUntilDate().toString(), bucketName.ssHotName(), documentEntity.getDocumentKey());
                                                    return Mono.error(new RetentionException(errMsg));
                                                }))
                                     .thenReturn(documentEntity);
                       } else if (
                           // 1. l'oggetto nel bucket non ha una retaintUntilDate impostata
                           //headOjectResponse.objectLockRetainUntilDate() == null
//								// 1. non si puo' distinguere il caso dell'impostazione di default da quella imposta dall'esterno o altro
//								//    Fare riferimento alla entity, piuttosto che all'oggetto presente nel bucket
//								documentEntity.getRetentionUntil() == null 
//								// 2. l'applicazione esterna NON sta chiedendo di modificare la retentionUntil
//								&& documentChanges.getRetentionUntil() == null
                           // 3. verifico i passaggi di stato utili
                               oldState != null && documentChanges.getDocumentState() != null
//								&& (documentChanges.getDocumentState().equalsIgnoreCase(AVAILABLE)
//										|| documentChanges.getDocumentState().equalsIgnoreCase(ATTACHED))
                               && ((oldState.equalsIgnoreCase(BOOKED) && documentChanges.getDocumentState().equalsIgnoreCase(AVAILABLE)) ||
                                   (oldState.equalsIgnoreCase(STAGED) && documentChanges.getDocumentState().equalsIgnoreCase(AVAILABLE)) ||
                                   (oldState.equalsIgnoreCase(AVAILABLE) &&
                                    documentChanges.getDocumentState().equalsIgnoreCase(ATTACHED)))) {
                           log.debug("setRetentionPeriodInBucketObjectMetadata() : rententionUtil " +
                                    " basata su variazione di stato {} - {}", oldState, documentChanges.getDocumentState());

                           Instant dataCreazioneObjectInBucket = headObjectResponse.lastModified();
                           log.debug("setRetentionPeriodInBucketObjectMetadata() : dataCreazioneObjectInBucket = {}",
                                    dataCreazioneObjectInBucket);
                           return getRetentionUntil(authPagopaSafestorageCxId,
                                                    authApiKey,
                                                    documentEntity.getDocumentKey(),
                                                    // si assume che la postDocument abbia gia' effettuato la traduzione dello stato logico
                                                    // e l'impostazione dello stesso stato nella entity
                                                    documentEntity.getDocumentLogicalState(),
                                                    // stato logico
                                                    documentEntity.getDocumentType().getTipoDocumento(),
                                                    dataCreazioneObjectInBucket).flatMap(instantRetentionUntil -> {
                                                                                    // aggiorno la retentionUntilDate nella entity
                                                                                    documentEntity.setRetentionUntil(FORMATTER.format(instantRetentionUntil));
                                                                                    // restituisco l'objectLockRetention per il
                                                                                    // successivo
                                                                                    // aggiornamento
                                                                                    // del metadato per
                                                                                    // l'object nel bucket
                                                                                    return Mono.just(ObjectLockRetention.builder()
                                                                                                                        .retainUntilDate(instantRetentionUntil)
                                                                                                                        .mode(objectLockRetentionMode)
                                                                                                                        .build());
                                                                                })
                                                                                .flatMap(objectLockRetention -> s3Service.putObjectRetention(documentEntity.getDocumentKey(), bucketName.ssHotName(), objectLockRetention)
                                                                                        .onErrorResume(S3Exception.class, throwable -> {
                                                                                            String errMsg = String.format("Error updating retention date '%s' from S3 bucket '%s' on document '%s'", objectLockRetention.retainUntilDate().toString(), bucketName.ssHotName(), documentEntity.getDocumentKey());
                                                                                            return Mono.error(new RetentionException(errMsg));
                                                                                        }))
                                                                                .onErrorResume(RetentionToIgnoreException.class, e ->
                                                                                {
                                                                                    log.debug(e.getMessage());
                                                                                    return Mono.empty();
                                                                                })
                                                                                .thenReturn(documentEntity);

                       }
                       return Mono.just(documentEntity);
                   })
                   // gestione errore
                   .doOnError(throwable -> log.error(LogUtils.ENDING_PROCESS_WITH_ERROR, SET_RETENTION_PERIOD_IN_BUCKET_OBJECT_METADATA, throwable, throwable.getMessage()))
                   .doOnSuccess(document -> log.info(LogUtils.SUCCESSFUL_OPERATION_LABEL,SET_RETENTION_PERIOD_IN_BUCKET_OBJECT_METADATA, document));
    }

}
