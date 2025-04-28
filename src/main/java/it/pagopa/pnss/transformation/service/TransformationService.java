package it.pagopa.pnss.transformation.service;

import it.pagopa.pn.library.sign.pojo.PnSignDocumentResponse;
import it.pagopa.pn.library.sign.service.impl.PnSignProviderService;
import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.DocumentResponse;
import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.TransformationMessage;
import it.pagopa.pnss.common.client.DocumentClientCall;
import it.pagopa.pnss.common.service.EventBridgeService;
import it.pagopa.pnss.common.service.SqsService;
import it.pagopa.pnss.common.utils.EventBridgeUtil;
import it.pagopa.pnss.configuration.TransformationConfig;
import it.pagopa.pnss.configurationproperties.BucketName;
import it.pagopa.pnss.configurationproperties.TransformationProperties;
import it.pagopa.pnss.transformation.exception.InvalidTransformationStateException;
import it.pagopa.pnss.transformation.model.dto.S3EventNotificationMessage;
import it.pagopa.pnss.transformation.service.S3Service;
import it.pagopa.pnss.transformation.utils.TransformationUtils;
import lombok.CustomLog;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequestEntry;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;

import java.time.Duration;
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
    @Value("${event.bridge.disponibilita-documenti-name}")
    private String disponibilitaDocumentiEventBridge;

    public TransformationService(S3Service s3Service,
                                 PnSignProviderService pnSignService,
                                 DocumentClientCall documentClientCall,
                                 BucketName bucketNames,
                                 SqsService sqsService,
                                 EventBridgeService eventBridgeService,
                                 TransformationConfig transformationConfig,
                                 TransformationProperties props) {
        this.s3Service = s3Service;
        this.pnSignService = pnSignService;
        this.documentClientCall = documentClientCall;
        this.bucketNames = bucketNames;
        this.sqsService = sqsService;
        this.eventBridgeService = eventBridgeService;
        this.transformationConfig = transformationConfig;
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
                                    return publishTransformationOnQueue(fileKey, sourceBucket, transformations.get(0), docContentType).then();
                                return handleObjectTag(fileKey, sourceBucket, tagOpt.get(), transformations, docContentType, document);
                            });
                });
    }

    private Mono<Void> handleObjectTag(String fileKey, String sourceBucket, Tag tag, List<String> transformations, String docContentType, DocumentResponse document) {
        log.debug(INVOKING_METHOD, HANDLE_OBJ_TAG, Stream.of(fileKey, sourceBucket, tag, transformations, docContentType, document).toList());
        String tagState = tag.value();
        return switch (tagState) {
            case OK -> endTransformation(tag.key(), fileKey, sourceBucket, transformations, docContentType);
            case ERROR -> sendUnavailabilityEvent(fileKey, sourceBucket, document);
            default -> Mono.error(new InvalidTransformationStateException.StatusNotRecognizedException(tagState));
        };
    }


    /*
     * Conclude il processo di trasformazione:
     *     - Determina il tipo di trasformazione attuale rimuovendo il prefisso "Transformation-" dal tag
     *     - Verifica se la trasformazione è presente nella lista delle trasformazioni previste
     *     - Se la trasformazione è stata completata:
     *          - Verifica se il file è già nel bucket finale
     *          - Se si, lo rimuove dal bucket temporaneo
     *          - Se no, lo carica nel bucket finale e poi lo rimuove dal bucket temporaneo
     */
    private Mono<Void> endTransformation(String tagKey, String fileKey, String sourceBucket, List<String> transformations, String docContentType) {
        String transformationType = tagKey.replace(TRANSFORMATION_TAG_PREFIX, "");
        return Mono.just(transformationType)
                .filter(transformations::contains)
                .switchIfEmpty(Mono.fromRunnable(() -> log.warn("Transformation {} is not recognized in the transformation list.", transformationType)))
                .flatMap(currentTransformation -> {
                    log.debug("Transformation completed for file: {}", fileKey);
                    return isAlreadyInBucket(fileKey)
                            .flatMap(isInFinalBucket -> Boolean.TRUE.equals(isInFinalBucket) ?
                                    removeObjectFromStagingBucket(fileKey, sourceBucket) :
                                    uploadToFinalBucket(fileKey, docContentType, sourceBucket));
                })
                .then();
    }


    // Invio sulla coda di trasformazione
    private Mono<SendMessageResponse> publishTransformationOnQueue(String fileKey, String sourceBucket,
                                                                   String transformationType,
                                                                   String docContentType) {
        log.debug(INVOKING_METHOD, PUBLISH_TRANSFORMATION_ON_QUEUE, Stream.of(fileKey, sourceBucket, transformationType, docContentType).toList());

        TransformationMessage message = new TransformationMessage();
        message.setFileKey(fileKey);
        message.setBucketName(sourceBucket);
        message.setContentType(docContentType);
        message.setTransformationType(transformationType);

        String queueName = transformationConfig.getTransformationQueueName(transformationType);
        log.info("Trasformation event {} sending to queue {}", transformationType, queueName);

        return sqsService.send(queueName, message);
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

    public Mono<PutObjectResponse> signAndTimemarkTransformation(TransformationMessage transformationMessage, boolean marcatura) {
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

}
