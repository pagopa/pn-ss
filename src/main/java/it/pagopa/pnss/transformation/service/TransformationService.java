package it.pagopa.pnss.transformation.service;


import io.awspring.cloud.messaging.listener.Acknowledgment;
import io.awspring.cloud.messaging.listener.SqsMessageDeletionPolicy;
import io.awspring.cloud.messaging.listener.annotation.SqsListener;
import it.pagopa.pn.commons.utils.MDCUtils;
import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.Document;
import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.DocumentResponse;
import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.DocumentType;
import it.pagopa.pnss.common.client.DocumentClientCall;
import it.pagopa.pnss.common.exception.IllegalTransformationException;
import it.pagopa.pnss.common.exception.InvalidStatusTransformationException;
import it.pagopa.pnss.common.service.SqsService;
import it.pagopa.pnss.common.utils.LogUtils;
import it.pagopa.pnss.configurationproperties.BucketName;
import it.pagopa.pnss.transformation.model.dto.CreatedS3ObjectDto;
import it.pagopa.pnss.transformation.rest.call.aruba.ArubaSignServiceCall;
import it.pagopa.pnss.transformation.service.impl.S3ServiceImpl;
import it.pagopa.pnss.transformation.wsdl.SignReturnV2;
import lombok.CustomLog;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.s3.model.*;

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

    private final ArubaSignServiceCall arubaSignServiceCall;
    private final S3ServiceImpl s3Service;
    private final DocumentClientCall documentClientCall;
    private final BucketName bucketName;
    private final SqsService sqsService;
    @Value("${default.internal.x-api-key.value:#{null}}")
    private String defaultInternalApiKeyValue;
    @Value("${default.internal.header.x-pagopa-safestorage-cx-id:#{null}}")
    private String defaultInternalClientIdValue;
    @Value("${s3.queue.sign-queue-name}")
    private String signQueueName;
    // Numero massimo di retry. Due step: 1) firma del documento e inserimento nel bucket 2) delete del file dal bucket di staging, piu' un retry aggiuntivo di sicurezza
    private static final int MAX_RETRIES = 3;

    public TransformationService(ArubaSignServiceCall arubaSignServiceCall, S3ServiceImpl s3Service,
                                 DocumentClientCall documentClientCall, BucketName bucketName, SqsService sqsService) {
        this.arubaSignServiceCall = arubaSignServiceCall;
        this.s3Service = s3Service;
        this.documentClientCall = documentClientCall;
        this.bucketName = bucketName;
        this.sqsService = sqsService;
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
                    return objectTransformation(fileKey, detailObject.getBucketOriginDetail().getName(), newStagingBucketObject.getRetry(), true);
                })
                .then()
                .doOnSuccess(s3ObjectDto -> acknowledgment.acknowledge())
                .doOnError(throwable -> !(throwable instanceof InvalidStatusTransformationException || throwable instanceof IllegalTransformationException), throwable -> log.error("An error occurred during transformations for document with key '{}' -> {}", fileKeyReference.get(), throwable.getMessage()))
                .onErrorResume(throwable -> newStagingBucketObject.getRetry() <= MAX_RETRIES, throwable -> {
                    newStagingBucketObject.setRetry(newStagingBucketObject.getRetry() + 1);
                    acknowledgment.acknowledge();
                    return sqsService.send(signQueueName, newStagingBucketObject).then();
                });
    }

    public Mono<DeleteObjectResponse> objectTransformation(String key, String stagingBucketName, int retry, Boolean marcatura) {
        log.debug(INVOKING_METHOD, OBJECT_TRANSFORMATION, Stream.of(key, stagingBucketName, marcatura).toList());
        return documentClientCall.getDocument(key)
                .map(DocumentResponse::getDocument)
                .filter(document -> document.getDocumentState().equalsIgnoreCase(STAGED) || document.getDocumentState().equalsIgnoreCase(AVAILABLE))
                .doOnDiscard(Document.class, document -> log.warn("Current status '{}' is not valid for transformation for document '{}'", document.getDocumentState(), key))
                .switchIfEmpty(Mono.error(new InvalidStatusTransformationException(key)))
                .filter(document -> {
                    var transformations = document.getDocumentType().getTransformations();
                    log.debug("Transformations list of document with key '{}' : {}", document.getDocumentKey(), transformations);
                    return transformations.contains(DocumentType.TransformationsEnum.SIGN_AND_TIMEMARK);
                })
                .switchIfEmpty(Mono.error(new IllegalTransformationException(key)))
                .filterWhen(document -> isSignatureNeeded(key, retry))
                .flatMap(document -> signDocument(document, key, stagingBucketName, marcatura))
                .flatMap(signReturnV2 -> s3Service.putObject(key, signReturnV2.getBinaryoutput(), bucketName.ssHotName()))
                .then(removeObjectFromStagingBucket(key, stagingBucketName));
    }

    private Mono<Boolean> isSignatureNeeded(String key, int retry) {
        if (retry == 0) return Mono.just(true);
        else return s3Service.headObject(key, bucketName.ssHotName())
                .thenReturn(false)
                .onErrorResume(NoSuchKeyException.class, throwable -> Mono.just(true));
    }

    private Mono<SignReturnV2> signDocument(Document document, String key, String stagingBucketName, boolean marcatura) {
        return s3Service.getObject(key, stagingBucketName)
                .flatMap(getObjectResponse -> {
                    var s3ObjectBytes = getObjectResponse.asByteArray();

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
                        signReturnV2.getDescription(), key));
    }

    private Mono<DeleteObjectResponse> removeObjectFromStagingBucket(String key, String stagingBucketName) {
        return Mono.defer(() -> s3Service.deleteObject(key, stagingBucketName)).onErrorResume(NoSuchKeyException.class, throwable -> Mono.empty());
    }

    private boolean isKeyPresent(CreatedS3ObjectDto createdS3ObjectDto) {
        var detailObject = createdS3ObjectDto.getCreationDetailObject();
        return detailObject != null && detailObject.getObject() != null && !StringUtils.isEmpty(detailObject.getObject().getKey());
    }
}
