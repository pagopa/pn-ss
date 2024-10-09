package it.pagopa.pnss.transformation.service;


import io.awspring.cloud.messaging.listener.Acknowledgment;
import io.awspring.cloud.messaging.listener.SqsMessageDeletionPolicy;
import io.awspring.cloud.messaging.listener.annotation.SqsListener;
import it.pagopa.pn.commons.utils.MDCUtils;
import it.pagopa.pn.library.sign.pojo.PnSignDocumentResponse;
import it.pagopa.pn.library.sign.service.impl.PnSignProviderService;
import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.Document;
import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.DocumentResponse;
import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.DocumentType;
import it.pagopa.pnss.common.client.DocumentClientCall;
import it.pagopa.pnss.common.exception.IllegalTransformationException;
import it.pagopa.pnss.common.exception.InvalidStatusTransformationException;
import it.pagopa.pnss.common.rest.call.pdfraster.PdfRasterCall;
import it.pagopa.pnss.common.service.SqsService;
import it.pagopa.pnss.common.utils.LogUtils;
import it.pagopa.pnss.configurationproperties.BucketName;
import it.pagopa.pnss.transformation.model.dto.CreatedS3ObjectDto;
import it.pagopa.pnss.transformation.service.impl.S3ServiceImpl;
import lombok.CustomLog;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.core.BytesWrapper;
import software.amazon.awssdk.services.s3.model.DeleteObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import static it.pagopa.pnss.common.constant.Constant.AVAILABLE;
import static it.pagopa.pnss.common.constant.Constant.STAGED;
import static it.pagopa.pnss.common.utils.LogUtils.*;
import static it.pagopa.pnss.common.utils.SqsUtils.logIncomingMessage;
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
    private final PdfRasterCall pdfRasterCall;
    private final Semaphore signAndTimemarkSemaphore;
    private final Semaphore rasterSemaphore;
    @Value("${default.internal.x-api-key.value:#{null}}")
    private String defaultInternalApiKeyValue;
    @Value("${default.internal.header.x-pagopa-safestorage-cx-id:#{null}}")
    private String defaultInternalClientIdValue;
    @Value("${s3.queue.sign-queue-name}")
    private String signQueueName;
    // Numero massimo di retry. Due step: 1) firma del documento e inserimento nel bucket 2) delete del file dal bucket di staging, piu' un retry aggiuntivo di sicurezza
    private static final int MAX_RETRIES = 3;

    public TransformationService(S3ServiceImpl s3Service,
                                 PnSignProviderService pnSignService,
                                 DocumentClientCall documentClientCall,
                                 BucketName bucketName,
                                 SqsService sqsService,
                                 PdfRasterCall pdfRasterCall,
                                 @Value("${transformation.max-thread-pool-size.sign-and-timemark}") Integer signAndTimemarkMaxThreadPoolSize,
                                 @Value("${transformation.max-thread-pool-size.raster}") Integer rasterMaxThreadPoolSize) {
        this.s3Service = s3Service;
        this.pnSignService = pnSignService;
        this.documentClientCall = documentClientCall;
        this.bucketName = bucketName;
        this.sqsService = sqsService;
        this.pdfRasterCall = pdfRasterCall;
        this.signAndTimemarkSemaphore = new Semaphore(signAndTimemarkMaxThreadPoolSize);
        this.rasterSemaphore = new Semaphore(rasterMaxThreadPoolSize);
    }

    @SqsListener(value = "${s3.queue.sign-queue-name}", deletionPolicy = SqsMessageDeletionPolicy.NEVER)
    void newStagingBucketObjectCreatedListener(CreatedS3ObjectDto newStagingBucketObject, Acknowledgment acknowledgment) {
        MDC.clear();
        var fileKey = isKeyPresent(newStagingBucketObject) ? newStagingBucketObject.getCreationDetailObject().getObject().getKey() : "null";
        MDC.put(MDC_CORR_ID_KEY, fileKey);
        log.logStartingProcess(NEW_STAGING_BUCKET_OBJECT_CREATED_LISTENER);
        MDCUtils.addMDCToContextAndExecute(newStagingBucketObjectCreatedEvent(newStagingBucketObject, acknowledgment)
                        .doOnSuccess(result -> log.logEndingProcess(NEW_STAGING_BUCKET_OBJECT_CREATED_LISTENER))
                        .doOnError(throwable -> log.logEndingProcess(NEW_STAGING_BUCKET_OBJECT_CREATED_LISTENER, false, throwable.getMessage())))
                .subscribe();
    }

    public Mono<Void> newStagingBucketObjectCreatedEvent(CreatedS3ObjectDto newStagingBucketObject, Acknowledgment acknowledgment) {

        log.debug(LogUtils.INVOKING_METHOD, NEW_STAGING_BUCKET_OBJECT_CREATED, newStagingBucketObject);

        AtomicReference<String> fileKeyReference = new AtomicReference<>("");
        return Mono.fromCallable(() -> {
                    logIncomingMessage(signQueueName, newStagingBucketObject);
                    return newStagingBucketObject;
                })
                .filter(this::isKeyPresent)
                .doOnDiscard(CreatedS3ObjectDto.class, createdS3ObjectDto -> log.debug("The new staging bucket object with id '{}' was discarded", newStagingBucketObject.getId()))
                .flatMap(createdS3ObjectDto -> {
                    var detailObject = createdS3ObjectDto.getCreationDetailObject();
                    var fileKey = detailObject.getObject().getKey();
                    fileKeyReference.set(fileKey);
                    return objectTransformation(fileKey, detailObject.getBucketOriginDetail().getName(), newStagingBucketObject.getRetry());
                })
                .then()
                .doOnSuccess( s3ObjectDto -> acknowledgment.acknowledge())
                .doOnError(throwable -> !(throwable instanceof InvalidStatusTransformationException || throwable instanceof IllegalTransformationException), throwable -> log.error("An error occurred during transformations for document with key '{}' -> {}", fileKeyReference.get(), throwable.getMessage()))
                .onErrorResume(throwable -> newStagingBucketObject.getRetry() <= MAX_RETRIES, throwable -> {
                    newStagingBucketObject.setRetry(newStagingBucketObject.getRetry() + 1);
                    return sqsService.send(signQueueName, newStagingBucketObject).then(Mono.fromRunnable(acknowledgment::acknowledge));
                });
    }

    public Mono<DeleteObjectResponse> objectTransformation(String key, String stagingBucketName, int retry) {
        log.debug(INVOKING_METHOD, OBJECT_TRANSFORMATION, Stream.of(key, stagingBucketName).toList());
        return documentClientCall.getDocument(key)
                .map(DocumentResponse::getDocument)
                .filter(document -> document.getDocumentState().equalsIgnoreCase(STAGED) || document.getDocumentState().equalsIgnoreCase(AVAILABLE))
                .doOnDiscard(Document.class, document -> log.warn("Current status '{}' is not valid for transformation for document '{}'", document.getDocumentState(), key))
                .switchIfEmpty(Mono.error(new InvalidStatusTransformationException(key)))
                .filter(document -> {
                    var transformations = document.getDocumentType().getTransformations();
                    log.debug("Transformations list of document with key '{}' : {}", document.getDocumentKey(), transformations);

                    return  transformations.contains(DocumentType.TransformationsEnum.SIGN) ||
                            transformations.contains(DocumentType.TransformationsEnum.SIGN_AND_TIMEMARK) ||
                            transformations.contains(DocumentType.TransformationsEnum.RASTER);
                })
                .switchIfEmpty(Mono.error(new IllegalTransformationException(key)))
                .filterWhen(document -> isSignatureNeeded(key, retry))
                .flatMap(document -> chooseTransformationType(document, key, stagingBucketName))
                .then(removeObjectFromStagingBucket(key, stagingBucketName));
    }

    private Mono<PutObjectResponse> chooseTransformationType(Document document, String key, String stagingBucketName) {
        var transformations = document.getDocumentType().getTransformations();

        if (transformations.contains(DocumentType.TransformationsEnum.SIGN_AND_TIMEMARK)) {
            return signAndTimemarkTransformation(document, key, stagingBucketName, true);
        } else if (transformations.contains(DocumentType.TransformationsEnum.SIGN)) {
            return signAndTimemarkTransformation(document, key, stagingBucketName, false);
        } else if (transformations.contains(DocumentType.TransformationsEnum.RASTER)) {
            return rasterTransformation(document, key, stagingBucketName);
        } else return Mono.error(new IllegalTransformationException(key));
    }

    private Mono<PutObjectResponse> signAndTimemarkTransformation(Document document, String key, String stagingBucketName, boolean marcatura) {
        log.debug(INVOKING_METHOD, SIGN_AND_TIMEMARK_TRANSFORMATION, Stream.of(document, key, stagingBucketName, marcatura).toList());
        acquireSemaphore(signAndTimemarkSemaphore);
        return signDocument(document, key, stagingBucketName, marcatura)
                .flatMap(pnSignDocumentResponse -> s3Service.putObject(key, pnSignDocumentResponse.getSignedDocument(), document.getContentType(), bucketName.ssHotName()))
                .doFinally(signalType -> signAndTimemarkSemaphore.release());
    }

    private Mono<PutObjectResponse> rasterTransformation(Document document, String key, String stagingBucketName) {
        log.debug(INVOKING_METHOD, RASTER_TRANSFORMATION, Stream.of(document, key, stagingBucketName).toList());
        acquireSemaphore(rasterSemaphore);
        return s3Service.getObject(key, stagingBucketName)
                .map(BytesWrapper::asByteArray)
                .flatMap(fileBytes -> pdfRasterCall.convertPdf(fileBytes, key))
                .flatMap(convertedDocument -> s3Service.putObject(key, convertedDocument, document.getContentType(), bucketName.ssHotName()))
                .doFinally(signalType -> rasterSemaphore.release());
    }

    private Mono<Boolean> isSignatureNeeded(String key, int retry) {
        if (retry == 0) return Mono.just(true);
        else return s3Service.headObject(key, bucketName.ssHotName())
                .thenReturn(false)
                .onErrorResume(NoSuchKeyException.class, throwable -> Mono.just(true));
    }

    private Mono<PnSignDocumentResponse> signDocument(Document document, String key, String stagingBucketName, boolean marcatura) {
        return s3Service.getObject(key, stagingBucketName)
                .flatMap(getObjectResponse -> {
                    var s3ObjectBytes = getObjectResponse.asByteArray();

                    log.debug("Content type of document with key '{}' : {}", document.getDocumentKey(), document.getContentType());

                    return switch (document.getContentType()) {
                        case APPLICATION_PDF_VALUE -> pnSignService.signPdfDocument(s3ObjectBytes, marcatura);
                        case APPLICATION_XML_VALUE -> pnSignService.signXmlDocument(s3ObjectBytes, marcatura);
                        default -> pnSignService.pkcs7Signature(s3ObjectBytes, marcatura);
                    };

                });
    }

    private Mono<DeleteObjectResponse> removeObjectFromStagingBucket(String key, String stagingBucketName) {
        return Mono.defer(() -> s3Service.deleteObject(key, stagingBucketName)).onErrorResume(NoSuchKeyException.class, throwable -> Mono.empty());
    }

    private boolean isKeyPresent(CreatedS3ObjectDto createdS3ObjectDto) {
        var detailObject = createdS3ObjectDto.getCreationDetailObject();
        return detailObject != null && detailObject.getObject() != null && !StringUtils.isEmpty(detailObject.getObject().getKey());
    }

    private void acquireSemaphore(Semaphore semaphore) {
        try {
            semaphore.acquire();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

}
