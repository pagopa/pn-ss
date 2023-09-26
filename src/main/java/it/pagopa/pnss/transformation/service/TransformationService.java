package it.pagopa.pnss.transformation.service;


import io.awspring.cloud.messaging.listener.Acknowledgment;
import io.awspring.cloud.messaging.listener.SqsMessageDeletionPolicy;
import io.awspring.cloud.messaging.listener.annotation.SqsListener;
import it.pagopa.pn.template.internal.rest.v1.dto.DocumentType;
import it.pagopa.pnss.common.client.DocumentClientCall;
import it.pagopa.pnss.common.client.exception.ArubaSignException;
import it.pagopa.pnss.common.client.exception.ArubaSignExceptionLimitCall;
import it.pagopa.pnss.common.constant.Constant;
import it.pagopa.pnss.configurationproperties.BucketName;
import it.pagopa.pnss.transformation.model.dto.CreatedS3ObjectDto;
import it.pagopa.pnss.transformation.rest.call.aruba.ArubaSignServiceCall;
import it.pagopa.pnss.transformation.service.impl.S3ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;
import java.util.concurrent.atomic.AtomicReference;
import static it.pagopa.pnss.common.constant.Constant.NOT_VALID_OBJECT_OR_KEY;
import static it.pagopa.pnss.common.utils.SqsUtils.logIncomingMessage;
import static org.springframework.http.MediaType.APPLICATION_PDF_VALUE;
import static org.springframework.http.MediaType.APPLICATION_XML_VALUE;


@Service
@Slf4j
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

    @SqsListener(value = "${s3.queue.sign-queue-name}", deletionPolicy = SqsMessageDeletionPolicy.NEVER)
    void newStagingBucketObjectCreatedListener(CreatedS3ObjectDto newStagingBucketObject, Acknowledgment acknowledgment) {
        newStagingBucketObjectCreatedEvent(newStagingBucketObject, acknowledgment).subscribe();
    }

    public Mono<Void> newStagingBucketObjectCreatedEvent(CreatedS3ObjectDto newStagingBucketObject, Acknowledgment acknowledgment) {

        AtomicReference<String> fileKeyReference = new AtomicReference<>("");

        return Mono.fromCallable(() -> {
                    logIncomingMessage(signQueueName, newStagingBucketObject);
                    return newStagingBucketObject;
                })
                .filter(createdS3ObjectDto -> {
                    var detailObject = createdS3ObjectDto.getCreationDetailObject();
                    return detailObject != null && detailObject.getObject() != null && !StringUtils.isEmpty(detailObject.getObject().getKey());
                })
                .doOnDiscard(CreatedS3ObjectDto.class,
                        createdS3ObjectDto -> log.debug("The new staging bucket object with id {} was discarded", newStagingBucketObject.getId()))
                .flatMap(createdS3ObjectDto -> {
                    var detailObject = createdS3ObjectDto.getCreationDetailObject();
                    var key = detailObject.getObject().getKey();
                    fileKeyReference.set(key);
                    return objectTransformation(key, detailObject.getBucketOriginDetail().getName(), true);
                })
                .doOnSuccess(s3ObjectDto -> acknowledgment.acknowledge())
                .doOnError(throwable -> log.error("An error occurred during transformations on file with key '{}' -> {}", fileKeyReference.get(), throwable.getMessage()));
    }

    public Mono<Void> objectTransformation(String key, String stagingBucketName, Boolean marcatura) {
        final String OBJECT_TRANSFORMATION = "TransformationService.objectTransformation()";

        log.debug(Constant.INVOKING_METHOD + Constant.ARG + Constant.ARG, OBJECT_TRANSFORMATION, key, stagingBucketName, marcatura);
        log.info(Constant.CLIENT_METHOD_INVOCATION + Constant.ARG, "s3Service.getObject()", key, stagingBucketName);
        return Mono.zipDelayError(documentClientCall.getDocument(key), s3Service.getObject(key, stagingBucketName))
                   .filter(objects -> {
                       var document = objects.getT1().getDocument();
                       var transformations = document.getDocumentType().getTransformations();
                       log.debug("Transformations list of document with key {}", document.getDocumentKey());
                       return transformations.contains(DocumentType.TransformationsEnum.SIGN_AND_TIMEMARK);
                   })
                   .flatMap(objects -> {
                       var document = objects.getT1().getDocument();
                       var s3ObjectBytes = objects.getT2().asByteArray();

                       log.debug("Content type of document with key {}", document.getDocumentKey());

                       return switch (document.getContentType()) {
                           case APPLICATION_PDF_VALUE -> arubaSignServiceCall.signPdfDocument(s3ObjectBytes, marcatura);
                           case APPLICATION_XML_VALUE -> arubaSignServiceCall.xmlSignature(s3ObjectBytes, marcatura);
                           default -> arubaSignServiceCall.pkcs7signV2(s3ObjectBytes, marcatura);
                       };
                   })
                   .doOnNext(signReturnV2 -> log.debug("Aruba sign service return status {}, return code {}, description {}",
                                                       signReturnV2.getStatus(),
                                                       signReturnV2.getReturnCode(),
                                                       signReturnV2.getDescription()))
                   .flatMap(signReturnV2 -> changeFromStagingBucketToHotBucket(key, signReturnV2.getBinaryoutput(), stagingBucketName))
                   .retryWhen(Retry.max(10).filter(ArubaSignException.class::isInstance).onRetryExhaustedThrow((retrySpec, retrySignal) -> {
                       throw new ArubaSignExceptionLimitCall(key);
                   }))
                   .then();
    }

    private Mono<Void> changeFromStagingBucketToHotBucket(String key, byte[] objectBytes, String stagingBucketName) {
        log.info(Constant.CLIENT_METHOD_INVOCATION + Constant.ARG + Constant.ARG, "s3Service.putObject()", key, objectBytes, bucketName.ssHotName());
        return s3Service.putObject(key, objectBytes, bucketName.ssHotName())
                        .flatMap(putObjectResponse -> {
                            log.info(Constant.CLIENT_METHOD_INVOCATION + Constant.ARG, "s3Service.deleteObject()", key, stagingBucketName);
                            return s3Service.deleteObject(key, stagingBucketName);
                        })
                        .then();
    }
}
