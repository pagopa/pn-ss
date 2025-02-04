package it.pagopa.pnss.transformation.service;


import it.pagopa.pn.library.sign.pojo.PnSignDocumentResponse;
import it.pagopa.pn.library.sign.service.impl.PnSignProviderService;
import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.Document;
import it.pagopa.pnss.common.client.DocumentClientCall;
import it.pagopa.pnss.common.service.SqsService;
import it.pagopa.pnss.configurationproperties.BucketName;
import it.pagopa.pnss.transformation.model.dto.CreatedS3ObjectDto;
import it.pagopa.pnss.transformation.service.impl.S3ServiceImpl;
import lombok.CustomLog;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.eventnotifications.s3.model.S3EventNotificationRecord;
import software.amazon.awssdk.services.s3.model.DeleteObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;

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

    public Mono<Void> handleS3Event(S3EventNotificationRecord record) {
        throw new NotImplementedException();
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

}
