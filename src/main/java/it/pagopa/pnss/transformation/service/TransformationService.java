package it.pagopa.pnss.transformation.service;


import io.awspring.cloud.messaging.listener.Acknowledgment;
import io.awspring.cloud.messaging.listener.SqsMessageDeletionPolicy;
import io.awspring.cloud.messaging.listener.annotation.SqsListener;
import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.Document;
import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.DocumentResponse;
import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.DocumentType;
import it.pagopa.pnss.common.client.DocumentClientCall;
import it.pagopa.pnss.common.client.exception.ArubaSignException;
import it.pagopa.pnss.common.client.exception.ArubaSignExceptionLimitCall;
import it.pagopa.pnss.common.utils.LogUtils;
import it.pagopa.pnss.configurationproperties.BucketName;
import it.pagopa.pnss.transformation.model.dto.CreatedS3ObjectDto;
import it.pagopa.pnss.transformation.rest.call.aruba.ArubaSignServiceCall;
import it.pagopa.pnss.transformation.service.impl.S3ServiceImpl;
import lombok.CustomLog;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Stream;

import static it.pagopa.pnss.common.constant.Constant.STAGED;
import static it.pagopa.pnss.common.utils.LogUtils.*;
import static it.pagopa.pnss.common.utils.SqsUtils.logIncomingMessage;
import static org.springframework.http.MediaType.APPLICATION_PDF_VALUE;
import static org.springframework.http.MediaType.APPLICATION_XML_VALUE;


@Service
@CustomLog
public class TransformationService {

    private final ArubaSignServiceCall arubaSignServiceCall;
    private final S3ServiceImpl s3Service;
    private final DocumentClientCall documentClientCall;
    private final BucketName bucketName;

    @Value("${s3.queue.sign-queue-name}")
    private String signQueueName;

    public TransformationService(ArubaSignServiceCall arubaSignServiceCall, S3ServiceImpl s3Service,
                                 DocumentClientCall documentClientCall, BucketName bucketName) {
        this.arubaSignServiceCall = arubaSignServiceCall;
        this.s3Service = s3Service;
        this.documentClientCall = documentClientCall;
        this.bucketName = bucketName;
    }
    private static Function<Mono<Void>, Mono<Void>> getRetryStrategy(String key) {
        return documentMono -> documentMono.retryWhen(Retry.backoff(3, Duration.ofSeconds(2))
                .filter(ArubaSignException.class::isInstance)
                .doBeforeRetry(retrySignal -> log.warn("Retry number {} for document with key '{}', caused by : {}", retrySignal.totalRetries(), key, retrySignal.failure().getMessage(), retrySignal.failure()))
                .onRetryExhaustedThrow((retrySpec, retrySignal) -> {
                    throw new ArubaSignExceptionLimitCall(key);
                }));
    }

    @SqsListener(value = "${s3.queue.sign-queue-name}", deletionPolicy = SqsMessageDeletionPolicy.NEVER)
    void newStagingBucketObjectCreatedListener(CreatedS3ObjectDto newStagingBucketObject, Acknowledgment acknowledgment) {
        MDC.clear();
        MDC.put(MDC_CORR_ID_KEY, newStagingBucketObject.getId());
        log.logStartingProcess(NEW_STAGING_BUCKET_OBJECT_CREATED_LISTENER);
        newStagingBucketObjectCreatedEvent(newStagingBucketObject, acknowledgment)
                .doOnSuccess(result -> log.logEndingProcess(NEW_STAGING_BUCKET_OBJECT_CREATED_LISTENER))
                .doOnError(throwable -> log.logEndingProcess(NEW_STAGING_BUCKET_OBJECT_CREATED_LISTENER, false, throwable.getMessage()))
                .subscribe();
    }

    public Mono<Void> newStagingBucketObjectCreatedEvent(CreatedS3ObjectDto newStagingBucketObject, Acknowledgment acknowledgment) {

        log.debug(LogUtils.INVOKING_METHOD, NEW_STAGING_BUCKET_OBJECT_CREATED, newStagingBucketObject);

        AtomicReference<String> fileKeyReference = new AtomicReference<>("");
        return Mono.fromCallable(() -> {
                    logIncomingMessage(signQueueName, newStagingBucketObject);
                    return newStagingBucketObject;
                })
                .filter(createdS3ObjectDto -> {
                    var detailObject = createdS3ObjectDto.getCreationDetailObject();
                    return detailObject != null && detailObject.getObject() != null && !StringUtils.isEmpty(detailObject.getObject().getKey());
                })
                .doOnDiscard(CreatedS3ObjectDto.class, createdS3ObjectDto -> log.debug("The new staging bucket object with id '{}' was discarded", newStagingBucketObject.getId()))
                .flatMap(createdS3ObjectDto -> {
                    var detailObject = createdS3ObjectDto.getCreationDetailObject();
                    var fileKey = detailObject.getObject().getKey();
                    fileKeyReference.set(fileKey);
                    return objectTransformation(fileKey, detailObject.getBucketOriginDetail().getName(), true);
                })
                .doOnSuccess(s3ObjectDto -> acknowledgment.acknowledge())
                .doOnError(throwable -> log.error("An error occurred during transformations for document with key '{}' -> {}", fileKeyReference.get(), throwable.getMessage()));
    }

    public Mono<Void> objectTransformation(String key, String stagingBucketName, Boolean marcatura) {

        log.debug(INVOKING_METHOD, OBJECT_TRANSFORMATION, Stream.of(key, stagingBucketName, marcatura).toList());
        return documentClientCall.getDocument(key)
                .map(DocumentResponse::getDocument)
                .filter(document -> {
                    var transformations = document.getDocumentType().getTransformations();
                    log.debug("Transformations list of document with key '{}' : {}", document.getDocumentKey(), transformations);
                    return transformations.contains(DocumentType.TransformationsEnum.SIGN_AND_TIMEMARK) && document.getDocumentState().equalsIgnoreCase(STAGED);
                })
                .doOnDiscard(Document.class, document -> log.debug("Document with key '{}' has been discarded", document.getDocumentKey()))
                .zipWhen(document -> s3Service.getObject(key, stagingBucketName))
                .flatMap(objects -> {
                    var document = objects.getT1();
                    var s3ObjectBytes = objects.getT2().asByteArray();

                    log.debug("Content type of document with key '{}' : {}", document.getDocumentKey(), document.getContentType());

                       return switch (document.getContentType()) {
                           case APPLICATION_PDF_VALUE -> arubaSignServiceCall.signPdfDocument(s3ObjectBytes, marcatura);
                           case APPLICATION_XML_VALUE -> arubaSignServiceCall.xmlSignature(s3ObjectBytes, marcatura);
                           default -> arubaSignServiceCall.pkcs7signV2(s3ObjectBytes, marcatura);
                       };
                   })
                   .doOnNext(signReturnV2 -> log.debug("Aruba sign service return status {}, return code {}, description {}, for document with key {}",
                                                       signReturnV2.getStatus(),
                                                       signReturnV2.getReturnCode(),
                                                       signReturnV2.getDescription(), key))
                   .flatMap(signReturnV2 -> changeFromStagingBucketToHotBucket(key, signReturnV2.getBinaryoutput(), stagingBucketName))
                   .transform(getRetryStrategy(key))
                   .doOnSuccess(result -> log.info(SUCCESSFUL_OPERATION_LABEL, OBJECT_TRANSFORMATION, result))
                   .then();
    }

    private Mono<Void> changeFromStagingBucketToHotBucket(String key, byte[] objectBytes, String stagingBucketName) {
        log.debug(INVOKING_METHOD, CHANGE_FROM_STAGING_BUCKET_TO_HOT_BUCKET, Stream.of(key, stagingBucketName).toList());
        return s3Service.putObject(key, objectBytes, bucketName.ssHotName())
                        .flatMap(putObjectResponse -> s3Service.deleteObject(key, stagingBucketName))
                        .doOnSuccess(result->log.info(SUCCESSFUL_OPERATION_LABEL, CHANGE_FROM_STAGING_BUCKET_TO_HOT_BUCKET, result))
                        .then();
    }
}
