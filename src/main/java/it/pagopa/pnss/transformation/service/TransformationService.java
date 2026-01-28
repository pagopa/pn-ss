package it.pagopa.pnss.transformation.service;

import it.pagopa.pn.library.exceptions.PnSpapiTemporaryErrorException;
import it.pagopa.pn.library.sign.pojo.PnSignDocumentResponse;
import it.pagopa.pn.library.sign.service.impl.PnSignProviderService;
import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.DocumentResponse;
import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.TransformationMessage;
import it.pagopa.pnss.common.client.DocumentClientCall;
import it.pagopa.pnss.common.service.EventBridgeService;
import it.pagopa.pnss.common.service.SqsService;
import it.pagopa.pnss.common.utils.EventBridgeUtil;
import it.pagopa.pnss.configuration.TransformationConfig;
import it.pagopa.pnss.configuration.sqs.SqsTimeoutProvider;
import it.pagopa.pnss.configurationproperties.BucketName;
import it.pagopa.pnss.configurationproperties.TransformationProperties;
import it.pagopa.pnss.transformation.exception.InvalidTransformationStateException;
import it.pagopa.pnss.transformation.model.dto.S3EventNotificationMessage;
import it.pagopa.pnss.transformation.utils.TransformationUtils;
import lombok.CustomLog;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequestEntry;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static it.pagopa.pnss.common.utils.LogUtils.*;
import static it.pagopa.pnss.common.utils.LogUtils.SUCCESSFUL_OPERATION_LABEL;
import static it.pagopa.pnss.configurationproperties.TransformationProperties.*;
import static it.pagopa.pnss.transformation.utils.TransformationUtils.*;
import static org.springframework.http.MediaType.APPLICATION_PDF_VALUE;
import static org.springframework.http.MediaType.APPLICATION_XML_VALUE;


@Service
@CustomLog
public class TransformationService {

    private final S3Service s3Service;
    private final PnSignProviderService pnSignService;
    private final DocumentClientCall documentClientCall;
    private final BucketName bucketNames;
    private final SqsService sqsService;
    private final EventBridgeService eventBridgeService;
    private final TransformationConfig transformationConfig;
    private final TransformationProperties props;
    private final SqsTimeoutProvider sqsTimeoutProvider;
    @Value("${event.bridge.disponibilita-documenti-name}")
    private String disponibilitaDocumentiEventBridge;

    public TransformationService(S3Service s3Service,
                                 PnSignProviderService pnSignService,
                                 DocumentClientCall documentClientCall,
                                 BucketName bucketNames,
                                 SqsService sqsService,
                                 EventBridgeService eventBridgeService,
                                 TransformationConfig transformationConfig,
                                 SqsTimeoutProvider sqsTimeoutProvider,
                                 TransformationProperties props) {
        this.s3Service = s3Service;
        this.pnSignService = pnSignService;
        this.documentClientCall = documentClientCall;
        this.bucketNames = bucketNames;
        this.sqsService = sqsService;
        this.eventBridgeService = eventBridgeService;
        this.transformationConfig = transformationConfig;
        this.sqsTimeoutProvider = sqsTimeoutProvider;
        this.props = props;
    }

    public Mono<Void> handleS3Event(S3EventNotificationMessage message) {
        log.debug(INVOKING_METHOD, HANDLE_S3_EVENT, message);
        String fileKey = message.getEventNotificationDetail().getObject().getKey();
        String sourceBucket = bucketNames.ssStageName();
        String eventType = message.getDetailType();

        if (!(isValidEventType(eventType, message.getEventNotificationDetail().getReason()))) {
            log.info("Skipping processing transformation for key {} with event type: {}", fileKey, eventType);
            return Mono.empty();
        }
        return documentClientCall.getDocument(fileKey)
                .flatMap(document -> {
                    List<String> transformations = document.getDocument().getDocumentType().getTransformations();
                    String docContentType = document.getDocument().getContentType();
                    return s3Service.getObjectTagging(fileKey, sourceBucket)
                            .flatMap(response -> {
                                Optional<Tag> tagOpt = response.tagSet().stream().filter(tag -> tag.key().startsWith(TRANSFORMATION_TAG_PREFIX)).findFirst();
                                if (tagOpt.isEmpty())
                                    return markInProgressAndPublishTransformationOnQueue(fileKey, sourceBucket, transformations.get(0), docContentType, transformations).then();
                                return  handleObjectTag(fileKey, sourceBucket, tagOpt.get(), transformations, docContentType, document);
                            }).onErrorResume(NoSuchKeyException.class, e -> {
                                log.info("Ignoring S3 Object Tags Added event for key {} because object no longer exists in staging bucket",fileKey);
                                return Mono.empty();
                            });
                });
    }

    private Mono<Void> handleObjectTag(String fileKey, String sourceBucket, Tag tag, List<String> transformations, String docContentType, DocumentResponse document) {
        log.debug(INVOKING_METHOD, HANDLE_OBJ_TAG, Stream.of(fileKey, sourceBucket, tag, transformations, docContentType, document).toList());
        String tagState = tag.value();
        return switch (tagState) {
            case TRANSFORMATION_IN_PROGRESS -> {
                log.info("Skipping processing for file {}: transformation {} is IN_PROGRESS", fileKey, tag.key());
                yield Mono.empty();
            }
            case OK -> handleNextTransformation(tag.key(), fileKey, sourceBucket, transformations, docContentType);
            case ERROR -> sendUnavailabilityEvent(fileKey, sourceBucket, document);
            default -> Mono.error(new InvalidTransformationStateException.StatusNotRecognizedException(tagState));
        };
    }

    /*
     * Nuova gestione della catena di trasformazioni, sostuisce il vecchio "endTransformation".
     * Non viene quindi interrotto il flusso alla prima trasformazione da applicare, ma
     * viene controllato se ci sono altre trasformazioni da applicare dal documentType.
     * Ogni trasformazione viene pubblicata sulla coda, ma SOLO alla fine di tutte le trasformazioni
     * viene caricato il file nel bucket finale di destinazione:
     *  - Determina il tipo di trasformazione attuale rimuovendo il prefisso "Transformation-" dal tag
     *  - Verifica se la trasformazione è presente nella lista delle trasformazioni previste
     *  - Verifica se è presente un'ulteriore trasformazione dopo quella corrente:
     *      - Se si, controlla se è già presente un file con la stessa trasformazione con tag OK sul bucket finale
     *        (meccanismo di idempotenza) e pubblica la trasformazione sulla coda. Il "ciclo" ricomincia
     *      - Se no, verifica che il file sia già nel bucket finale:
     *          - Se si, lo rimuove dal bucket temporaneo
     *          - Se no, lo carica nel bucket finale e poi lo rimuove dal bucket temporaneo
     */
    private Mono<Void> handleNextTransformation(String tagKey, String fileKey, String sourceBucket, List<String> transformations, String docContentType) {
        String transformationType = tagKey.replace(TRANSFORMATION_TAG_PREFIX, "");
        return Mono.just(transformationType)
                .filter(transformations::contains)
                .switchIfEmpty(Mono.fromRunnable(() ->
                        log.warn("Transformation {} is not recognized in the transformation list.", transformationType)
                )).flatMap(transformation ->
                        processTransformation(transformation, fileKey, sourceBucket, transformations, docContentType)).then();
    }

    /*
     * Gestisce la trasformazione corrente di un file e decide se passare alla trasformazione successiva o completare il flusso
     */
    private Mono<Void> processTransformation(String currentTransformation, String fileKey, String sourceBucket, List<String> transformations, String docContentType) {
        int currentIndex = transformations.indexOf(currentTransformation);
        int nextIndex = currentIndex + 1;
        log.info("Transformation={} completed for file {}", currentTransformation, fileKey);
        // esiste una trasformazione successiva?
        if (nextIndex < transformations.size()) {
            return handleNextInChain(transformations.get(nextIndex), fileKey, sourceBucket, docContentType, transformations);
        }
        // se non ci sono altre trasformazioni nella catena
        return handleLastTransformation(fileKey, sourceBucket, docContentType);
    }

    /*
     * Gestisce la prossima trasformazione nella catena.
     * Controlla se la trasformazione può essere eseguita (idempotenza) e pubblica il messaggio sulla coda se necessario senza inviarlo al bucket finale
     */
    private Mono<Void> handleNextInChain(String nextTransformation, String fileKey, String sourceBucket, String docContentType, List<String> transformations) {
        log.info("Next transformation {} detected for file {}", nextTransformation, fileKey);
        // richiamiamo canBeTransformed per garantire idempotenza in caso di doppio tag OK, controlliamo quindi se già è presente nel bucket finale
        return canBeTransformed(fileKey, sourceBucket, nextTransformation)
                .flatMap(canTransform -> {
                    if (Boolean.TRUE.equals(canTransform)) {
                        log.info("Publishing next transformation {} for file {}", nextTransformation, fileKey);
                        return markInProgressAndPublishTransformationOnQueue(fileKey, sourceBucket, nextTransformation, docContentType, transformations);
                    }
                    log.info("Skipping transformation {} for file {} already processed", nextTransformation, fileKey);
                    return Mono.empty();
                }).then();
    }

    /*
     * Gestisce l'ultimo step della catena di trasformazioni e controlla se il file è già nel bucket finale, decidendo
     *  se rimuoverlo dallo staging o caricarlo nel bucket finale (uploadToFinalBucket fa anche la cancellazione da quello di staging)
     */
    private Mono<Void> handleLastTransformation(String fileKey, String sourceBucket, String docContentType) {
        log.info("Last transformation completed for file {}", fileKey);
        return isAlreadyInBucket(fileKey)
                .flatMap(isInFinalBucket -> {
                    if (Boolean.TRUE.equals(isInFinalBucket)) {
                        log.info("File {} already in final bucket, removing from staging", fileKey);
                        return removeObjectFromStagingBucket(fileKey, sourceBucket);
                    }
                    log.info("Uploading file {} to final bucket", fileKey);
                    return uploadToFinalBucket(fileKey, docContentType, sourceBucket);
                }).then();
    }

    //Aggiunge un tag Transformation-XXX=inProgress e poi fa la pubblicazione sulla coda
    private Mono<SendMessageResponse> markInProgressAndPublishTransformationOnQueue(String fileKey, String sourceBucket, String transformationType, String docContentType, List<String>transformations) {
        log.debug(INVOKING_METHOD, MARK_IN_PROGRESS_AND_PUBLISH_TRANSFORMATION, Stream.of(fileKey, sourceBucket, transformationType, docContentType).toList());
        log.info("Marking transformation {} as IN_PROGRESS for file {}", transformationType, fileKey);
        List<Tag> tagSet = new ArrayList<>();
        for (String t : transformations) {
            if (t.equals(transformationType)) {
                tagSet.add(Tag.builder().key(TRANSFORMATION_TAG_PREFIX + t).value(TRANSFORMATION_IN_PROGRESS).build());
                break; // la trasformazione corrente è inProgress, tutte le successive non vengono taggate (ancora)
            } else {
                tagSet.add(Tag.builder().key(TRANSFORMATION_TAG_PREFIX + t).value(OK).build());
            }
        }
        log.info("FileKey {} has this tagSet= {}", fileKey, tagSet);
        return s3Service.putObjectTagging(fileKey, sourceBucket, Tagging.builder().tagSet(tagSet).build())
                .then(Mono.defer(() -> {
                    TransformationMessage message = new TransformationMessage();
                    message.setFileKey(fileKey);
                    message.setBucketName(sourceBucket);
                    message.setContentType(docContentType);
                    message.setTransformationType(transformationType);

                    String queueName = transformationConfig.getTransformationQueueName(transformationType);
                    log.info("Sending transformation {} to queue {}", transformationType, queueName);

                    return sqsService.send(queueName, message);
                })
        );
    }

    private Mono<DeleteObjectsResponse> uploadToFinalBucket(String fileKey, String contentType, String sourceBucket) {
        log.debug(INVOKING_METHOD, UPLOAD_FINAL_BUCKET, Stream.of(fileKey, contentType, sourceBucket).toList());
        return s3Service.getObject(fileKey, sourceBucket)
                .flatMap(responseByte -> {
                    byte[] fileBytes = responseByte.asByteArray();
                    return s3Service.putObject(fileKey, fileBytes, contentType, bucketNames.ssHotName())
                            .flatMap(putResponse -> removeObjectFromStagingBucket(fileKey, sourceBucket));
                });
    }


    // Verifica se il file è già presente nel bucket finale
    private Mono<Boolean> isAlreadyInBucket(String key) {
        return s3Service.headObject(key, bucketNames.ssHotName())
                .thenReturn(true)
                .onErrorResume(NoSuchKeyException.class, throwable -> Mono.just(false));
    }

    private Mono<DeleteObjectsResponse> removeObjectFromStagingBucket(String key, String stagingBucketName) {
        return s3Service.listObjectVersions(key, stagingBucketName)
                .flatMapMany(response -> Flux.fromIterable(response.versions()))
                .map(objectVersion -> ObjectIdentifier.builder().key(objectVersion.key()).versionId(objectVersion.versionId()).build())
                .collectList()
                .flatMap(identifiers -> s3Service.deleteObjectVersions(key, stagingBucketName, identifiers));
    }


    private Mono<Void> sendUnavailabilityEvent(String fileKey, String sourceBucket, DocumentResponse documentResponse) {
        log.warn("Transformation error detected for file: {} in bucket: {}", fileKey, sourceBucket);
        return Mono.just(documentResponse)
                .flatMap(TransformationUtils::mapToDocumentEntity)
                .flatMap(documentEntity -> {
                    PutEventsRequestEntry event = EventBridgeUtil.createUnavailabilityMessage(documentEntity, fileKey, disponibilitaDocumentiEventBridge);
                    return eventBridgeService.putSingleEvent(event);
                })
                .then();
    }

    public Mono<PutObjectResponse> signAndTimemarkTransformation(TransformationMessage transformationMessage, boolean marcatura,String queueName) {
        log.debug(INVOKING_METHOD, SIGN_AND_TIMEMARK_TRANSFORMATION, Stream.of(transformationMessage, marcatura).toList());
        String fileKey = transformationMessage.getFileKey();
        String contentType = transformationMessage.getContentType();
        String bucketName = transformationMessage.getBucketName();
        String transformationType = transformationMessage.getTransformationType();
        return signDocument(fileKey, contentType, bucketName, marcatura)
                .flatMap(pnSignDocumentResponse ->
                        canBeTransformed(fileKey, bucketName, transformationType)
                                .flatMap(canTransform -> {
                                    if (canTransform) {
                                        Tagging tagging = buildTransformationTagging(transformationType, OK);
                                        return s3Service.putObject(fileKey, pnSignDocumentResponse.getSignedDocument(), contentType, bucketName, tagging);
                                    } else {
                                        log.info("File {} already transformed, skipping.", fileKey);
                                        return Mono.just(PutObjectResponse.builder().build()); // placeholder per tipo coerente
                                    }
                                })
                )
                .timeout(sqsTimeoutProvider.getTimeoutForQueue(queueName))
                .onErrorResume(err -> handleSignError(transformationMessage, fileKey, bucketName, transformationType, err))
                .doOnSuccess(resp -> log.debug("Successfully completed signAndTimemarkTransformation for fileKey={}", fileKey));
    }

    private Mono<PutObjectResponse> handleSignError(TransformationMessage transformationMessage, String fileKey, String bucketName, String transformationType, Throwable error) {
        log.info("Invoking handleSignError for fileKey={} for error cause {}, message={}", fileKey, error.getCause() != null ? error.getCause().toString() : "none", error.getMessage());
        if (isPermanentException.test(error)) {
            log.info("Permanent error found for fileKey={}", fileKey);
            return handlePermanentTransformationException(fileKey, bucketName, transformationType, error).then(Mono.just(PutObjectResponse.builder().build()));
        } else {
            log.info("Error={} found for fileKey={} proceeding to invoke temporaryException", error.getMessage(), fileKey);
            return handleTemporaryTransformationException(transformationMessage,fileKey, bucketName, transformationType, error).then(Mono.just(PutObjectResponse.builder().build()));
        }
    }

    public Mono<PutObjectTaggingResponse> dummyTransformation(TransformationMessage transformationMessage) {
        log.debug(INVOKING_METHOD, DUMMY_TRANSFORMATION, transformationMessage);
        String fileKey = transformationMessage.getFileKey();
        String bucketName = transformationMessage.getBucketName();
        String transformationType = transformationMessage.getTransformationType();
        return Mono.delay(Duration.ofMillis(props.getDummyDelay()))
                .filterWhen(unused -> canBeTransformed(fileKey, bucketName, transformationType))
                .flatMap(unused -> s3Service.putObjectTagging(fileKey, bucketName, buildTransformationTagging(transformationType, OK)))
                .doOnSuccess(result -> log.debug(SUCCESSFUL_OPERATION_LABEL, DUMMY_TRANSFORMATION, result));
    }

    // Meccanismo di idempotenza. Se il file è già presente sul bucket con il tag Transformation-xxx=OK, non lo carico di nuovo.
    private Mono<Boolean> canBeTransformed(String key, String bucketName, String transformation) {
        return s3Service.getObjectTagging(key, bucketName)
                .flatMapIterable(GetObjectTaggingResponse::tagSet)
                .filter(tag -> tag.key().equals(TRANSFORMATION_TAG_PREFIX + transformation))
                .filter(tag -> "OK".equals(tag.value()))
                .next()
                .doOnNext(tag -> log.debug("Found tag {} for key '{}'. Skipping {} transformation.", tag, key, transformation))
                .map(tag -> false)
                .defaultIfEmpty(true);
    }

    private Mono<PutObjectTaggingResponse> handlePermanentTransformationException(String fileKey, String bucketName, String transformationType, Throwable throwable) {
        log.error(EXCEPTION_IN_TRANSFORMATION, transformationType, throwable);
        return s3Service.putObjectTagging(fileKey, bucketName, buildTransformationTagging(transformationType, ERROR));
    }

    //LONG RETRY: fare in modo di leggere il messaggio e aggiungere un metadata RETRY, se becco una PnSpapiTemporaryErrorException o MaxRetry incremento fino al massimo (10) altrimenti la marco come error
    private Mono<PutObjectTaggingResponse> handleTemporaryTransformationException(TransformationMessage transformationMessage, String fileKey, String bucketName, String transformationType, Throwable throwable) {
        log.info("Invoking handleTemporaryTransformationException for fileKey={} and transformationMessage={}", fileKey, transformationMessage);
        int retry = transformationMessage.getRetry();
        log.info("TransformationMessage has retry={}", retry);
        if (retry < TRANSFORMATION_MAX_RETRY) {
            transformationMessage.setRetry(retry + 1);
            log.info("TemporaryErrorException received retry {}/{} for fileKey={}", retry + 1, TRANSFORMATION_MAX_RETRY, fileKey, throwable);
            return sqsService.send(transformationConfig.getTransformationQueueName(transformationType), transformationMessage)
                    .then(Mono.empty());
        }
        //max retry raggiunto
        log.error("Max retry exceeded for fileKey={}", fileKey, throwable);
        return handlePermanentTransformationException(fileKey, bucketName, transformationType, throwable);
    }

    private Mono<PnSignDocumentResponse> signDocument(String fileKey, String contentType, String bucketName, boolean marcatura) {
        return s3Service.getObject(fileKey, bucketName)
                .flatMap(getObjectResponse -> {
                    var s3ObjectBytes = getObjectResponse.asByteArray();

                    log.debug("Content type of document with key '{}' : {}", fileKey, contentType);

                    return switch (contentType) {
                        case APPLICATION_PDF_VALUE -> pnSignService.signPdfDocument(s3ObjectBytes, marcatura);
                        case APPLICATION_XML_VALUE -> pnSignService.signXmlDocument(s3ObjectBytes, marcatura);
                        default -> pnSignService.pkcs7Signature(s3ObjectBytes, marcatura);
                    };

                });
    }

}
