package it.pagopa.pnss.transformation.service;

import it.pagopa.pn.library.sign.pojo.PnSignDocumentResponse;
import it.pagopa.pn.library.sign.service.impl.PnSignProviderService;
import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.TransformationMessage;
import it.pagopa.pnss.common.exception.CadesContentMismatchException;
import it.pagopa.pnss.common.service.EventBridgeService;
import it.pagopa.pnss.common.service.SqsService;
import it.pagopa.pnss.common.client.DocumentClientCall;
import it.pagopa.pnss.configuration.TransformationConfig;
import it.pagopa.pnss.configuration.sqs.SqsTimeoutProvider;
import it.pagopa.pnss.configurationproperties.BucketName;
import it.pagopa.pnss.configurationproperties.TransformationProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

import java.time.Duration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static it.pagopa.pnss.transformation.utils.TransformationUtils.TRANSFORMATION_MAX_RETRY;

@ExtendWith(MockitoExtension.class)
class TransformationServiceSignDocumentTest {

    @Mock
    private S3Service s3Service;
    @Mock
    private PnSignProviderService pnSignService;
    @Mock
    private DocumentClientCall documentClientCall;
    @Mock
    private SqsService sqsService;
    @Mock
    private EventBridgeService eventBridgeService;
    @Mock
    private TransformationConfig transformationConfig;
    @Mock
    private SqsTimeoutProvider sqsTimeoutProvider;
    @Mock
    private TransformationProperties props;

    private TransformationService transformationService;

    private static final byte[] ORIGINAL_BYTES = "original zip content".getBytes();
    private static final byte[] SIGNED_BYTES = "signed but different content".getBytes();
    private static final String FILE_KEY = "test-file.zip";
    private static final String BUCKET_NAME = "staging-bucket";
    private static final String CONTENT_TYPE_ZIP = "application/zip";

    @BeforeEach
    void setUp() {
        BucketName bucketName = new BucketName("hot-bucket", BUCKET_NAME);
        transformationService = new TransformationService(
                s3Service,
                pnSignService,
                documentClientCall,
                bucketName,
                sqsService,
                eventBridgeService,
                transformationConfig,
                sqsTimeoutProvider,
                props
        );
        ReflectionTestUtils.setField(transformationService, "disponibilitaDocumentiEventBridge", "test-event-bridge");
    }

    @Test
    void signAndTimemarkTransformation_cadesContentMismatch_maxRetryExhausted_doesNotEmitEventBridge() {
        TransformationMessage message = new TransformationMessage();
        message.setFileKey(FILE_KEY);
        message.setContentType(CONTENT_TYPE_ZIP);
        message.setBucketName(BUCKET_NAME);
        message.setTransformationType("SIGN");
        message.setRetry(TRANSFORMATION_MAX_RETRY);

        ResponseBytes<GetObjectResponse> responseBytes = ResponseBytes.fromByteArray(
                GetObjectResponse.builder().build(),
                ORIGINAL_BYTES
        );
        when(s3Service.getObject(anyString(), anyString())).thenReturn(Mono.just(responseBytes));

        PnSignDocumentResponse signResponse = new PnSignDocumentResponse();
        signResponse.setSignedDocument(SIGNED_BYTES);
        when(pnSignService.pkcs7Signature(any(), anyBoolean())).thenReturn(Mono.just(signResponse));

        when(sqsTimeoutProvider.getTimeoutForQueue(anyString())).thenReturn(Duration.ofSeconds(30));

        Mono<Void> result = transformationService.signAndTimemarkTransformation(message, false, "test-queue")
                .then();

        StepVerifier.create(result)
                .verifyComplete();

        verify(eventBridgeService, never()).putSingleEvent(any());
    }

    @Test
    void signDocument_zipContentType_signedBytesNotMatchOriginal_throwsCadesContentMismatchException() {
        ResponseBytes<GetObjectResponse> responseBytes = ResponseBytes.fromByteArray(
                GetObjectResponse.builder().build(),
                ORIGINAL_BYTES
        );
        when(s3Service.getObject(anyString(), anyString())).thenReturn(Mono.just(responseBytes));

        PnSignDocumentResponse signResponse = new PnSignDocumentResponse();
        signResponse.setSignedDocument(SIGNED_BYTES);
        when(pnSignService.pkcs7Signature(any(), anyBoolean())).thenReturn(Mono.just(signResponse));

        @SuppressWarnings("unchecked")
        Mono<PnSignDocumentResponse> result = (Mono<PnSignDocumentResponse>) ReflectionTestUtils.invokeMethod(
                transformationService, "signDocument", FILE_KEY, CONTENT_TYPE_ZIP, BUCKET_NAME, false
        );

        StepVerifier.create(result)
                .expectError(CadesContentMismatchException.class)
                .verify(Duration.ofSeconds(5));
    }
}
