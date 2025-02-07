package it.pagopa.pnss.transformation.service;


import it.pagopa.pn.library.exceptions.PnSpapiPermanentErrorException;
import it.pagopa.pn.library.sign.pojo.PnSignDocumentResponse;
import it.pagopa.pn.library.sign.service.impl.PnSignProviderService;
import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.Document;
import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.DocumentResponse;
import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.DocumentType;
import it.pagopa.pnss.common.client.DocumentClientCall;
import it.pagopa.pnss.common.service.EventBridgeService;
import it.pagopa.pnss.common.service.SqsService;
import it.pagopa.pnss.common.utils.EventBridgeUtil;
import it.pagopa.pnss.configuration.TransformationConfig;
import it.pagopa.pnss.configurationproperties.BucketName;
import it.pagopa.pnss.configurationproperties.TransformationProperties;
import it.pagopa.pnss.repositorymanager.entity.DocumentEntity;
import it.pagopa.pnss.transformation.exception.InvalidDocumentStateException;
import it.pagopa.pnss.transformation.exception.InvalidTransformationStateException;
import it.pagopa.pnss.transformation.model.dto.TransformationMessage;
import it.pagopa.pnss.transformation.service.impl.S3ServiceImpl;
import lombok.CustomLog;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.eventnotifications.s3.model.S3EventNotificationRecord;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequestEntry;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;

import java.time.Duration;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static it.pagopa.pnss.common.utils.LogUtils.*;
import static it.pagopa.pnss.common.utils.LogUtils.SUCCESSFUL_OPERATION_LABEL;
import static it.pagopa.pnss.configurationproperties.TransformationProperties.*;
import static org.springframework.http.MediaType.APPLICATION_PDF_VALUE;
import static org.springframework.http.MediaType.APPLICATION_XML_VALUE;


@Service
@CustomLog
public class TransformationService {

    private final S3ServiceImpl s3Service;
    private final PnSignProviderService pnSignService;
    private final DocumentClientCall documentClientCall;
    private final BucketName bucketName;
    private final SqsService sqsService;
    private final EventBridgeService eventBridgeService;
    private final TransformationConfig transformationConfig;
    private final TransformationProperties props;
    @Value("${default.internal.x-api-key.value:#{null}}")
    private String defaultInternalApiKeyValue;
    @Value("${default.internal.header.x-pagopa-safestorage-cx-id:#{null}}")
    private String defaultInternalClientIdValue;
    public static final String OBJECT_CREATED_PUT_EVENT = "s3:ObjectCreated:Put";
    public static final String OBJECT_TAGGING_PUT_EVENT = "s3:ObjectTagging:Put";
    @Value("${event.bridge.disponibilita-documenti-name}")
    private String disponibilitaDocumentiEventBridge;
    @Value("${s3.queue.sign-queue-name}")
    private String signQueueName;
    @Value("${pn.ss.transformation-service.max.messages}")
    private int maxMessages;

    private static final List<Class<? extends Throwable>> PERMANENT_TRASNFORMATION_EXCEPTIONS = List.of(PnSpapiPermanentErrorException.class);
    private final Predicate<Throwable> isPermanentException = e -> PERMANENT_TRASNFORMATION_EXCEPTIONS.contains(e.getClass());

    public TransformationService(S3ServiceImpl s3Service,
                                 PnSignProviderService pnSignService,
                                 DocumentClientCall documentClientCall,
                                 BucketName bucketName,
                                 SqsService sqsService,
                                 EventBridgeService eventBridgeService,
                                 TransformationConfig transformationConfig,
                                 TransformationProperties props) {
        this.s3Service = s3Service;
        this.pnSignService = pnSignService;
        this.documentClientCall = documentClientCall;
        this.bucketName = bucketName;
        this.sqsService = sqsService;
        this.eventBridgeService = eventBridgeService;
        this.transformationConfig = transformationConfig;
        this.props = props;
    }

    public Mono<Void> handleS3Event(S3EventNotificationRecord record) {
        String fileKey = record.getS3().getObject().getKey();
        String sourceBucket = bucketName.ssStageName();
        String eventType = record.getEventName();


        if (!OBJECT_CREATED_PUT_EVENT.equals(eventType) && !OBJECT_TAGGING_PUT_EVENT.equals(eventType)) {
            log.info("Skipping processing for event type: {}", eventType);
            return Mono.empty();
        }

        return documentClientCall.getDocument(fileKey)
                .flatMap(document -> {
                    if (!isValidState(document.getDocument().getDocumentState())) {
                        log.error("Document state is not valid: {}", document.getDocument().getDocumentState());
                        return Mono.error(new InvalidDocumentStateException(document.getDocument().getDocumentState(), fileKey));
                    }

                    List<DocumentType.TransformationsEnum> docTransformationList = document.getDocument().getDocumentType().getTransformations();
                    List<String> transformationNames = docTransformationList.stream()
                            .map(DocumentType.TransformationsEnum::name)
                            .toList();

                    String docContentType = document.getDocument().getContentType();

                    return s3Service.getObjectTagging(fileKey, sourceBucket)
                            .flatMap(response -> {
                                if (!response.hasTagSet() || response.tagSet().isEmpty()) {
                                    return publishTransformationOnQueue(fileKey, sourceBucket, transformationNames.get(0), docContentType).then();
                                } else {
                                    log.info("No tags are present for document: {}", fileKey);
                                    return handleObjectTag(fileKey, sourceBucket, response, transformationNames, docContentType, document);
                                }
                            });
                });
    }

    private Mono<Void> handleObjectTag(String fileKey, String sourceBucket, GetObjectTaggingResponse tagsResponse, List<String> transformationNames, String docContentType, DocumentResponse document) {
        return Flux.fromIterable(tagsResponse.tagSet())
                .filter(tag -> tag.key().startsWith("Transformation-"))
                .flatMap(tag -> {
                    String tagStatus = tag.value();
                    return switch (tagStatus) {
                        case "OK" -> handleTagTransformationEvent(tag.key(), fileKey, sourceBucket, transformationNames, docContentType);
                        case "ERROR" -> sendIndisponibilitaEvent(fileKey, sourceBucket, document);
                        default -> Mono.error(new InvalidTransformationStateException.StatusNotRecognizedException("Unrecognized transformation state: " + tagStatus));
                    };
                })
                .next()
                .switchIfEmpty(Mono.empty());
    }


    /*
     * Gestisce l'evento di trasformazione di un file basato sul tag associato all'oggetto S3:
     *     - Determina il tipo di trasformazione attuale rimuovendo il prefisso "Transformation-" dal tag
     *     - Verifica se la trasformazione è presente nella lista delle trasformazioni previste
     *     - Se esiste una trasformazione successiva, pubblica un messaggio nella coda per avviarla
     *     - Se tutte le trasformazioni sono completate:
     *          - Verifica se il file è già nel bucket finale
     *          - Se si, lo rimuove dal bucket temporaneo
     *          - Se no, lo carica nel bucket finale e poi lo rimuove dal bucket temporaneo
     */
    private Mono<Void> handleTagTransformationEvent(String tagKey, String fileKey, String sourceBucket, List<String> transformationNames, String docContentType) {
        String transformationType = tagKey.replace("Transformation-", "");

        return Mono.just(transformationType)
                .filter(transformationNames::contains)
                .switchIfEmpty(Mono.fromRunnable(() -> log.warn("Transformation {} is not recognized in the transformation list.", transformationType)))
                .flatMap(currentTransformation -> {
                    int currentIndexTransformation = transformationNames.indexOf(currentTransformation);
                    if (currentIndexTransformation + 1 < transformationNames.size()) {
                        String nextTransformation = transformationNames.get(currentIndexTransformation + 1);
                        log.info("Next transformation: {}", nextTransformation);
                        return publishTransformationOnQueue(fileKey, sourceBucket, nextTransformation, docContentType);
                    } else {
                        log.info("All transformations are completed for file: {}", fileKey);
                        return isAlreadyInBucket(fileKey)
                                .flatMap(isInFinalBucket -> isInFinalBucket ?
                                        removeObjectFromStagingBucket(fileKey, sourceBucket) :
                                        uploadToFinalBucket(fileKey, docContentType, sourceBucket));
                    }
                })
                .then();
    }


    // Invio sulla coda di trasformazione
    private Mono<SendMessageResponse> publishTransformationOnQueue(String fileKey, String sourceBucket,
                                                                   String transformationType,
                                                                   String docContentType) {

        TransformationMessage message = TransformationMessage.builder()
                .fileKey(fileKey)
                .bucketName(sourceBucket)
                .contentType(docContentType)
                .transformationType(transformationType)
                .build();

        String queueName = transformationConfig.getTransformationQueueName(transformationType);

        return sqsService.send(queueName, message);
    }


    private Mono<DeleteObjectsResponse> uploadToFinalBucket(String fileKey, String contentType, String sourceBucket) {
        return s3Service.getObject(fileKey, sourceBucket)
                .flatMap(responseByte -> {
                    byte[] fileBytes = responseByte.asByteArray();
                    return s3Service.putObject(fileKey, fileBytes, contentType, bucketName.ssHotName())
                            .flatMap(putResponse -> removeObjectFromStagingBucket(fileKey, sourceBucket));
                });
    }


    // Verifica se il file è già presente nel bucket finale
    private Mono<Boolean> isAlreadyInBucket(String key) {
        return s3Service.headObject(key, bucketName.ssHotName())
                .thenReturn(true)
                .onErrorResume(NoSuchKeyException.class, throwable -> Mono.just(false));
    }

    public Mono<PutObjectResponse> signAndTimemarkTransformation(it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.TransformationMessage transformationMessage, boolean marcatura) {
        log.debug(INVOKING_METHOD, SIGN_AND_TIMEMARK_TRANSFORMATION, Stream.of(transformationMessage, marcatura).toList());
        String fileKey = transformationMessage.getFileKey();
        String contentType = transformationMessage.getContentType();
        String bucketName = transformationMessage.getBucketName();
        String transformationType = transformationMessage.getTransformationType();
        return signDocument(fileKey, contentType, bucketName, marcatura)
                .filterWhen(unused -> canBeTransformed(fileKey, bucketName, transformationType))
                .flatMap(pnSignDocumentResponse -> {
                    Tagging tagging = buildTransformationTagging(transformationType, OK);
                    return s3Service.putObject(fileKey, pnSignDocumentResponse.getSignedDocument(), contentType, bucketName, tagging);
                })
                .doOnSuccess(result -> log.debug(SUCCESSFUL_OPERATION_LABEL, SIGN_AND_TIMEMARK_TRANSFORMATION, result))
                .onErrorResume(isPermanentException, error -> handlePermanentTransformationException(fileKey, bucketName, transformationType, error).then(Mono.empty()));
    }

    public Mono<PutObjectTaggingResponse> dummyTransformation(it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.TransformationMessage transformationMessage) {
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
                .next()
                .doOnNext(tag -> log.debug("Found tag {} for key '{}'. Skipping {} transformation.", tag, key, transformation))
                .map(tag -> false)
                .defaultIfEmpty(true);
    }

    private Mono<PutObjectTaggingResponse> handlePermanentTransformationException(String fileKey, String bucketName, String transformationType, Throwable throwable) {
        log.error(EXCEPTION_IN_TRANSFORMATION, transformationType, throwable);
        return s3Service.putObjectTagging(fileKey, bucketName, buildTransformationTagging(transformationType, ERROR));
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

    private Mono<DeleteObjectsResponse> removeObjectFromStagingBucket(String key, String stagingBucketName) {
        return s3Service.listObjectVersions(key, stagingBucketName)
                .flatMapMany(response -> Flux.fromIterable(response.versions()))
                .flatMap(version -> s3Service.deleteObjectVersions(key, stagingBucketName, version.versionId()))
                .next();
    }


    private Mono<Void> sendIndisponibilitaEvent(String fileKey, String sourceBucket, DocumentResponse documentResponse) {
        log.warn("Transformation error detected for file: {} in bucket: {}", fileKey, sourceBucket);

        return Mono.just(documentResponse)
                .flatMap(this::mapToDocumentEntity)
                .flatMap(documentEntity -> {
                    PutEventsRequestEntry event = EventBridgeUtil.createMessageForIndisponibilita(documentEntity, fileKey, disponibilitaDocumentiEventBridge);
                    return eventBridgeService.putSingleEvent(event);
                })
                .doOnSuccess(response -> log.info("Transformation event ERROR sent successfully for file: {}", fileKey))
                .doOnError(error -> log.error("Failed to send transformation event ERROR for file: {}", fileKey, error))
                .then();
    }


    private boolean isValidState(String stato) {
        return "STAGED".equalsIgnoreCase(stato) || "AVAILABLE".equalsIgnoreCase(stato);
    }

    private Mono<DocumentEntity> mapToDocumentEntity(DocumentResponse documentResponse) {
        return Mono.fromSupplier(() -> {
            DocumentEntity documentEntity = new DocumentEntity();
            documentEntity.setDocumentKey(documentResponse.getDocument().getDocumentKey());
            documentEntity.setDocumentState(documentResponse.getDocument().getDocumentState());
            documentEntity.setContentType(documentResponse.getDocument().getContentType());
            documentEntity.setClientShortCode(documentResponse.getDocument().getClientShortCode());
            documentEntity.setCheckSum(documentResponse.getDocument().getCheckSum());
            return documentEntity;
        });
    }

    private Tagging buildTransformationTagging(String transformation, String value) {
        return Tagging.builder().tagSet(Tag.builder().key(TRANSFORMATION_TAG_PREFIX + transformation).value(value).build()).build();
    }

}
