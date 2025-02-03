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
import it.pagopa.pnss.common.utils.CompareUtils;
import it.pagopa.pnss.common.utils.LogUtils;
import it.pagopa.pnss.configurationproperties.BucketName;
import it.pagopa.pnss.transformation.model.dto.CreatedS3ObjectDto;
import it.pagopa.pnss.transformation.model.dto.CreationDetail;
import it.pagopa.pnss.transformation.model.dto.TransformationMessage;
import it.pagopa.pnss.transformation.service.impl.S3ServiceImpl;
import lombok.CustomLog;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
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
    @Value("${default.internal.x-api-key.value:#{null}}")
    private String defaultInternalApiKeyValue;
    @Value("${default.internal.header.x-pagopa-safestorage-cx-id:#{null}}")
    private String defaultInternalClientIdValue;
    @Value("${s3.queue.sign-queue-name}")
    private String signQueueName;
    @Value("${pn.ss.transformation-service.max.messages}")
    private int maxMessages;
    @Value("${pn.ss.transformation-service.dummy.delay:250}")
    private Integer dummyDelay;
    // Numero massimo di retry. Due step: 1) firma del documento e inserimento nel bucket 2) delete del file dal bucket di staging, piu' un retry aggiuntivo di sicurezza
    private static final int MAX_RETRIES = 3;

    public TransformationService(S3ServiceImpl s3Service,
                                 PnSignProviderService pnSignService,
                                 DocumentClientCall documentClientCall,
                                 BucketName bucketName,
                                 SqsService sqsService) {
        this.s3Service = s3Service;
        this.pnSignService = pnSignService;
        this.documentClientCall = documentClientCall;
        this.bucketName = bucketName;
        this.sqsService = sqsService;
    }

    public Mono<PutObjectResponse> signAndTimemarkTransformation(TransformationMessage transformationMessage) {
        throw new NotImplementedException();
    }

    public Mono<PutObjectResponse> signTransformation(TransformationMessage transformationMessage) {
        throw new NotImplementedException();
    }

    public Mono<PutObjectResponse> dummyTransformation(TransformationMessage transformationMessage) {
        throw new NotImplementedException();
    }

    private Mono<Boolean> isSignatureNeeded(String key, int retry) {
        if (retry == 0) return Mono.just(true);
        else return s3Service.headObject(key, bucketName.ssHotName())
                .thenReturn(false)
                .onErrorResume(NoSuchKeyException.class, throwable -> Mono.just(true));
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

    private Mono<DeleteObjectResponse> removeObjectFromStagingBucket(String key, String stagingBucketName) {
        return Mono.defer(() -> s3Service.deleteObject(key, stagingBucketName)).onErrorResume(NoSuchKeyException.class, throwable -> Mono.empty());
    }

    private boolean isKeyPresent(CreatedS3ObjectDto createdS3ObjectDto) {
        var detailObject = createdS3ObjectDto.getCreationDetailObject();
        return detailObject != null && detailObject.getObject() != null && !StringUtils.isEmpty(detailObject.getObject().getKey());
    }

    private void waitDelay() {
        try {
            Thread.sleep(dummyDelay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

}
