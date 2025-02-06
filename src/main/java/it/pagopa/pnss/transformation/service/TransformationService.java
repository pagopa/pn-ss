package it.pagopa.pnss.transformation.service;

import it.pagopa.pn.library.exceptions.PnSpapiPermanentErrorException;
import it.pagopa.pn.library.sign.pojo.PnSignDocumentResponse;
import it.pagopa.pn.library.sign.service.impl.PnSignProviderService;
import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.TransformationMessage;
import it.pagopa.pnss.common.client.DocumentClientCall;
import it.pagopa.pnss.common.service.SqsService;
import it.pagopa.pnss.configurationproperties.TransformationProperties;
import it.pagopa.pnss.transformation.service.impl.S3ServiceImpl;
import lombok.CustomLog;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.s3.model.*;

import java.time.Duration;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static it.pagopa.pnss.common.utils.LogUtils.*;
import static it.pagopa.pnss.configurationproperties.TransformationProperties.*;
import static org.springframework.http.MediaType.APPLICATION_PDF_VALUE;
import static org.springframework.http.MediaType.APPLICATION_XML_VALUE;


@Service
@CustomLog
public class TransformationService {

    private final S3ServiceImpl s3Service;
    private final PnSignProviderService pnSignService;
    private final DocumentClientCall documentClientCall;
    private final SqsService sqsService;
    private final TransformationProperties props;
    @Value("${default.internal.x-api-key.value:#{null}}")
    private String defaultInternalApiKeyValue;
    @Value("${default.internal.header.x-pagopa-safestorage-cx-id:#{null}}")
    private String defaultInternalClientIdValue;
    @Value("${s3.queue.sign-queue-name}")
    private String signQueueName;
    @Value("${pn.ss.transformation-service.max.messages}")
    private int maxMessages;

    public TransformationService(S3ServiceImpl s3Service,
                                 PnSignProviderService pnSignService,
                                 DocumentClientCall documentClientCall,
                                 SqsService sqsService, TransformationProperties props) {
        this.s3Service = s3Service;
        this.pnSignService = pnSignService;
        this.documentClientCall = documentClientCall;
        this.sqsService = sqsService;
        this.props = props;
    }

    private static final List<Class<? extends Throwable>> PERMANENT_TRASNFORMATION_EXCEPTIONS = List.of(PnSpapiPermanentErrorException.class);
    private final Predicate<Throwable> isPermanentException = e -> PERMANENT_TRASNFORMATION_EXCEPTIONS.contains(e.getClass());

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

    private Tagging buildTransformationTagging(String transformation, String value) {
        return Tagging.builder().tagSet(Tag.builder().key(TRANSFORMATION_TAG_PREFIX + transformation).value(value).build()).build();
    }

}
