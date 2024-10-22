package it.pagopa.pnss.transformation.service;


import it.pagopa.pn.commons.utils.MDCUtils;
import it.pagopa.pn.library.sign.pojo.PnSignDocumentResponse;
import it.pagopa.pn.library.sign.service.impl.PnSignProviderService;
import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.Document;
import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.DocumentResponse;
import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.DocumentType;
import it.pagopa.pnss.common.client.DocumentClientCall;
import it.pagopa.pnss.common.exception.IllegalTransformationException;
import it.pagopa.pnss.common.exception.InvalidStatusTransformationException;
import it.pagopa.pnss.common.model.pojo.SqsMessageWrapper;
import it.pagopa.pnss.common.rest.call.pdfraster.PdfRasterCall;
import it.pagopa.pnss.common.service.SqsService;
import it.pagopa.pnss.common.utils.LogUtils;
import it.pagopa.pnss.configurationproperties.BucketName;
import it.pagopa.pnss.transformation.model.dto.CreatedS3ObjectDto;
import it.pagopa.pnss.transformation.model.dto.CreationDetail;
import it.pagopa.pnss.transformation.service.impl.S3ServiceImpl;
import lombok.CustomLog;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.core.BytesWrapper;
import software.amazon.awssdk.services.s3.model.DeleteObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.sqs.model.DeleteMessageResponse;

import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
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
    @Value("${pn.ss.transformation-service.max.messages}")
    private int maxMessages;
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

    @Scheduled(cron = "${PnEcCronScaricamentoEsitiPec ?:*/10 * * * * *}")
    void newStagingBucketObjectCreatedListener() {
        MDC.clear();
        log.logStartingProcess(NEW_STAGING_BUCKET_OBJECT_CREATED_LISTENER);
        AtomicBoolean hasMessages = new AtomicBoolean();
        hasMessages.set(true);
        Mono.defer(() -> sqsService.getMessages(signQueueName, CreatedS3ObjectDto.class, maxMessages)
                        .doOnNext(messageWrapper -> logIncomingMessage(signQueueName,  messageWrapper.getMessageContent()))
                        .flatMap(sqsMessageWrapper -> {
                            String fileKey = isKeyPresent(sqsMessageWrapper.getMessageContent()) ? sqsMessageWrapper.getMessageContent().getCreationDetailObject().getObject().getKey() : "null";
                            MDC.put(MDC_CORR_ID_KEY, fileKey);
                            return MDCUtils.addMDCToContextAndExecute(newStagingBucketObjectCreatedEvent(sqsMessageWrapper));
                        })
                        .collectList())
                        .doOnNext(list -> hasMessages.set(!list.isEmpty()))
                        .repeat(hasMessages::get)
                        .doOnError(e -> log.logEndingProcess(NEW_STAGING_BUCKET_OBJECT_CREATED_LISTENER, false, e.getMessage()))
                        .doOnComplete(() -> log.logEndingProcess(NEW_STAGING_BUCKET_OBJECT_CREATED_LISTENER))
                        .blockLast();
    }

    public Mono<DeleteMessageResponse> newStagingBucketObjectCreatedEvent(SqsMessageWrapper<CreatedS3ObjectDto> newStagingBucketObjectWrapper) {

        log.debug(LogUtils.INVOKING_METHOD, NEW_STAGING_BUCKET_OBJECT_CREATED, newStagingBucketObjectWrapper.getMessageContent());

        AtomicReference<String> fileKeyReference = new AtomicReference<>("");
        return Mono.fromCallable(() -> {
                    return  newStagingBucketObjectWrapper;
                })
                .filter(wrapper-> isKeyPresent(wrapper.getMessageContent()))
                .doOnDiscard(SqsMessageWrapper.class, wrapper -> {
                    CreatedS3ObjectDto newStagingBucketObject = (CreatedS3ObjectDto) wrapper.getMessageContent();
                    log.debug("The new staging bucket object with id '{}' was discarded", newStagingBucketObject.getId());
                })
                .flatMap(wrapper -> {
                    CreationDetail detailObject = wrapper.getMessageContent().getCreationDetailObject();
                    String fileKey = detailObject.getObject().getKey();
                    fileKeyReference.set(fileKey);
                    return objectTransformation(fileKey, detailObject.getBucketOriginDetail().getName(), wrapper.getMessageContent().getRetry(), true);
                })
                .doOnError(throwable -> log.debug(EXCEPTION_IN_PROCESS + ": {}", NEW_STAGING_BUCKET_OBJECT_CREATED, throwable.getMessage(), throwable))
                .flatMap(transformationResponse ->
                        sqsService.deleteMessageFromQueue(newStagingBucketObjectWrapper.getMessage(), signQueueName)
                                .doOnSuccess(response -> log.debug("Message with key '{}' was deleted", fileKeyReference.get()))
                                .doOnError(throwable -> log.error("An error occurred during deletion of message with key '{}' -> {}", fileKeyReference.get(), throwable.getMessage()))
                )
                .doOnError(throwable -> !(throwable instanceof InvalidStatusTransformationException || throwable instanceof IllegalTransformationException), throwable -> log.error("An error occurred during transformations for document with key '{}' -> {}", fileKeyReference.get(), throwable.getMessage()))
                .onErrorResume(throwable -> newStagingBucketObjectWrapper.getMessageContent().getRetry() <= MAX_RETRIES, throwable -> {
                    newStagingBucketObjectWrapper.getMessageContent().setRetry(newStagingBucketObjectWrapper.getMessageContent().getRetry() + 1);
                    return sqsService.send(signQueueName, newStagingBucketObjectWrapper.getMessageContent())
                            .then(sqsService.deleteMessageFromQueue(newStagingBucketObjectWrapper.getMessage(), signQueueName))
                            .doOnSuccess(response -> log.debug("Retry - Message with key '{}' was deleted", fileKeyReference.get()))
                            .doOnError(throwable1 -> log.error("Retry - An error occurred during deletion of message with key '{}' -> {}", fileKeyReference.get(), throwable1.getMessage()));
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
                    return !transformations.isEmpty();
                })
                .switchIfEmpty(Mono.error(new IllegalTransformationException(key)))
                .filterWhen(document -> isSignatureNeeded(key, retry))
                .flatMap(document -> chooseTransformationType(document, key, stagingBucketName))
                .then(removeObjectFromStagingBucket(key, stagingBucketName))
                .doOnSuccess(response -> log.debug(SUCCESSFUL_OPERATION_LABEL, OBJECT_TRANSFORMATION, response))
                .doOnError(throwable -> log.debug(EXCEPTION_IN_PROCESS + ": {}", OBJECT_TRANSFORMATION, throwable.getMessage(), throwable));
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
                .doOnError(throwable -> log.debug(EXCEPTION_IN_PROCESS + ": {}", RASTER_TRANSFORMATION, throwable.getMessage(), throwable))
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
