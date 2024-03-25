package it.pagopa.pnss.transformation.service;

import io.awspring.cloud.messaging.listener.Acknowledgment;
import it.pagopa.pn.library.sign.configurationproperties.PnSignServiceConfigurationProperties;
import it.pagopa.pn.library.sign.pojo.PnSignDocumentResponse;
import it.pagopa.pn.library.sign.service.impl.AlternativeSignProviderService;
import it.pagopa.pn.library.sign.service.impl.ArubaSignProviderService;
import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.*;
import it.pagopa.pnss.common.DocTypesConstant;
import it.pagopa.pnss.common.client.DocumentClientCall;
import it.pagopa.pn.library.sign.exception.aruba.ArubaSignException;
import it.pagopa.pnss.common.exception.InvalidStatusTransformationException;
import it.pagopa.pnss.common.service.SqsService;
import it.pagopa.pnss.configurationproperties.BucketName;
import it.pagopa.pnss.testutils.annotation.SpringBootTestWebEnv;
import it.pagopa.pnss.transformation.model.dto.BucketOriginDetail;
import it.pagopa.pnss.transformation.model.dto.CreatedS3ObjectDto;
import it.pagopa.pnss.transformation.model.dto.CreationDetail;
import it.pagopa.pnss.transformation.model.dto.S3Object;
import it.pagopa.pnss.transformation.wsdl.SignReturnV2;
import lombok.CustomLog;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

import static it.pagopa.pnss.common.constant.Constant.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@SpringBootTestWebEnv
@CustomLog
public class TransformationServiceTest {

    @SpyBean
    private TransformationService transformationService;
    @MockBean
    private DocumentClientCall documentClientCall;
    @MockBean
    private ArubaSignProviderService arubaSignProviderService;
    @MockBean
    private AlternativeSignProviderService alternativeSignProviderService;
    @Autowired
    private BucketName bucketName;
    @Autowired
    private S3Client s3TestClient;
    @Autowired
    private PnSignServiceConfigurationProperties pnSignServiceConfigurationProperties;
    @SpyBean
    private SqsService sqsService;
    @Value("${s3.queue.sign-queue-name}")
    private String signQueueName;

    private final String FILE_KEY = "FILE_KEY";
    private static final String PROVIDER_SWITCH = "providerSwitch";
    private static final String ARUBA_PROVIDER = "1999-01-01T10:00:00Z;aruba";
    private static final String ALTERNATIVE_PROVIDER = "2022-01-01T10:00:00Z;alternative";

    @AfterEach
    void afterEach() {
        ReflectionTestUtils.setField(pnSignServiceConfigurationProperties, PROVIDER_SWITCH, ARUBA_PROVIDER);
    }

    @BeforeEach
    void initialize() {
        putObjectInBucket(FILE_KEY, bucketName.ssStageName(), new byte[10]);
    }

    @AfterEach
    void clean() {
        deleteObjectInBucket(FILE_KEY, bucketName.ssHotName());
    }

    @Test
    void newStagingBucketObjectCreatedEventBlankDetail() {

        CreatedS3ObjectDto createdS3ObjectDto = new CreatedS3ObjectDto();

        Acknowledgment acknowledgment = new Acknowledgment() {
            @Override
            public Future<?> acknowledge() {
                return null;
            }
        };

        var testMono = transformationService.newStagingBucketObjectCreatedEvent(createdS3ObjectDto, acknowledgment);

        StepVerifier.create(testMono).expectNextCount(0).verifyComplete();
    }

    @Test
    void newStagingBucketObjectCreatedInvalidStatus() {

        S3Object s3Object = new S3Object();
        s3Object.setKey(FILE_KEY);

        BucketOriginDetail bucketOriginDetail = new BucketOriginDetail();
        bucketOriginDetail.setName(bucketName.ssStageName());

        CreationDetail creationDetail = new CreationDetail();
        creationDetail.setObject(s3Object);
        creationDetail.setBucketOriginDetail(bucketOriginDetail);

        CreatedS3ObjectDto createdS3ObjectDto = new CreatedS3ObjectDto();
        createdS3ObjectDto.setCreationDetailObject(creationDetail);

        Acknowledgment acknowledgment = new Acknowledgment() {
            @Override
            public Future<?> acknowledge() {
                return null;
            }
        };

        mockGetDocument("application/pdf", BOOKED, List.of(DocumentType.TransformationsEnum.SIGN_AND_TIMEMARK));
        var testMono = transformationService.newStagingBucketObjectCreatedEvent(createdS3ObjectDto, acknowledgment);

        StepVerifier.create(testMono).verifyComplete();
        verify(transformationService, times(1)).objectTransformation(anyString(), anyString(), anyInt(), anyBoolean());
        verify(sqsService, times(1)).send(eq(signQueueName), any(CreatedS3ObjectDto.class));
    }

    @Test
    void newStagingBucketObjectMaxRetriesExceeded() {

        S3Object s3Object = new S3Object();
        s3Object.setKey(FILE_KEY);

        BucketOriginDetail bucketOriginDetail = new BucketOriginDetail();
        bucketOriginDetail.setName(bucketName.ssStageName());

        CreationDetail creationDetail = new CreationDetail();
        creationDetail.setObject(s3Object);
        creationDetail.setBucketOriginDetail(bucketOriginDetail);

        CreatedS3ObjectDto createdS3ObjectDto = new CreatedS3ObjectDto();
        createdS3ObjectDto.setCreationDetailObject(creationDetail);

        createdS3ObjectDto.setRetry(4);
        Acknowledgment acknowledgment = new Acknowledgment() {
            @Override
            public Future<?> acknowledge() {
                return null;
            }
        };

        mockGetDocument("application/pdf", BOOKED, List.of(DocumentType.TransformationsEnum.SIGN_AND_TIMEMARK));
        var testMono = transformationService.newStagingBucketObjectCreatedEvent(createdS3ObjectDto, acknowledgment);

        StepVerifier.create(testMono).expectError(InvalidStatusTransformationException.class).verify();
        verify(transformationService, times(1)).objectTransformation(anyString(), anyString(), anyInt(), anyBoolean());
        verify(sqsService, times(0)).send(eq(signQueueName), any(CreatedS3ObjectDto.class));
    }

    @Test
    void newStagingBucketObjectCreatedInvalidTransformation() {

        S3Object s3Object = new S3Object();
        s3Object.setKey(FILE_KEY);

        BucketOriginDetail bucketOriginDetail = new BucketOriginDetail();
        bucketOriginDetail.setName(bucketName.ssStageName());

        CreationDetail creationDetail = new CreationDetail();
        creationDetail.setObject(s3Object);
        creationDetail.setBucketOriginDetail(bucketOriginDetail);

        CreatedS3ObjectDto createdS3ObjectDto = new CreatedS3ObjectDto();
        createdS3ObjectDto.setCreationDetailObject(creationDetail);

        Acknowledgment acknowledgment = new Acknowledgment() {
            @Override
            public Future<?> acknowledge() {
                return null;
            }
        };

        mockGetDocument("application/pdf", STAGED, List.of(DocumentType.TransformationsEnum.NONE));
        var testMono = transformationService.newStagingBucketObjectCreatedEvent(createdS3ObjectDto, acknowledgment);

        StepVerifier.create(testMono).verifyComplete();
        verify(transformationService, times(1)).objectTransformation(anyString(), anyString(), anyInt(), anyBoolean());
        verify(sqsService, times(1)).send(eq(signQueueName), any(CreatedS3ObjectDto.class));
    }

    @Test
    void newStagingBucketObjectCreatedEventArubaKo() {

        S3Object s3Object = new S3Object();
        s3Object.setKey(FILE_KEY);

        BucketOriginDetail bucketOriginDetail = new BucketOriginDetail();
        bucketOriginDetail.setName(bucketName.ssStageName());

        CreationDetail creationDetail = new CreationDetail();
        creationDetail.setObject(s3Object);
        creationDetail.setBucketOriginDetail(bucketOriginDetail);

        CreatedS3ObjectDto createdS3ObjectDto = new CreatedS3ObjectDto();
        createdS3ObjectDto.setCreationDetailObject(creationDetail);

        Acknowledgment acknowledgment = new Acknowledgment() {
            @Override
            public Future<?> acknowledge() {
                return null;
            }
        };

        mockGetDocument("application/pdf", STAGED, List.of(DocumentType.TransformationsEnum.SIGN_AND_TIMEMARK));
        when(arubaSignProviderService.signPdfDocument(any(), anyBoolean())).thenReturn(Mono.error(new ArubaSignException()));
        var testMono = transformationService.newStagingBucketObjectCreatedEvent(createdS3ObjectDto, acknowledgment);

        StepVerifier.create(testMono).verifyComplete();
        verify(sqsService, times(1)).send(eq(signQueueName), any(CreatedS3ObjectDto.class));
    }

    @ParameterizedTest
    @ValueSource(strings = {"application/pdf", "application/xml", "other"})
    void newStagingBucketObjectCreatedEvent_ArubaProvider_Ok(String contentType) {

        S3Object s3Object = new S3Object();
        s3Object.setKey(FILE_KEY);

        BucketOriginDetail bucketOriginDetail = new BucketOriginDetail();
        bucketOriginDetail.setName(bucketName.ssStageName());

        CreationDetail creationDetail = new CreationDetail();
        creationDetail.setObject(s3Object);
        creationDetail.setBucketOriginDetail(bucketOriginDetail);

        CreatedS3ObjectDto createdS3ObjectDto = new CreatedS3ObjectDto();
        createdS3ObjectDto.setCreationDetailObject(creationDetail);

        Acknowledgment acknowledgment = new Acknowledgment() {
            @Override
            public Future<?> acknowledge() {
                return null;
            }
        };

        mockGetDocument(contentType, STAGED, List.of(DocumentType.TransformationsEnum.SIGN_AND_TIMEMARK));
        mockSignCalls();
        var testMono = transformationService.newStagingBucketObjectCreatedEvent(createdS3ObjectDto, acknowledgment);

        StepVerifier.create(testMono).expectNextCount(0).verifyComplete();
        verify(transformationService, times(1)).objectTransformation(anyString(), anyString(), anyInt(), anyBoolean());
        verifyArubaProviderSignCalls(contentType);
    }

    @ParameterizedTest
    @ValueSource(strings = {"application/pdf", "application/xml", "other"})
    void newStagingBucketObjectCreatedEvent_AlternativeProvider_Ok(String contentType) {

        S3Object s3Object = new S3Object();
        s3Object.setKey(FILE_KEY);

        BucketOriginDetail bucketOriginDetail = new BucketOriginDetail();
        bucketOriginDetail.setName(bucketName.ssStageName());

        CreationDetail creationDetail = new CreationDetail();
        creationDetail.setObject(s3Object);
        creationDetail.setBucketOriginDetail(bucketOriginDetail);

        CreatedS3ObjectDto createdS3ObjectDto = new CreatedS3ObjectDto();
        createdS3ObjectDto.setCreationDetailObject(creationDetail);

        Acknowledgment acknowledgment = new Acknowledgment() {
            @Override
            public Future<?> acknowledge() {
                return null;
            }
        };

        ReflectionTestUtils.setField(pnSignServiceConfigurationProperties, PROVIDER_SWITCH, ALTERNATIVE_PROVIDER);
        mockGetDocument(contentType, STAGED, List.of(DocumentType.TransformationsEnum.SIGN_AND_TIMEMARK));
        mockSignCalls();
        var testMono = transformationService.newStagingBucketObjectCreatedEvent(createdS3ObjectDto, acknowledgment);

        StepVerifier.create(testMono).expectNextCount(0).verifyComplete();
        verify(transformationService, times(1)).objectTransformation(anyString(), anyString(), anyInt(), anyBoolean());
        verifyAlternativeProviderSignCalls(contentType);
    }

    @Test
    void newStagingBucketObjectCreatedEventRetryNotInHotBucketOk() {

        S3Object s3Object = new S3Object();
        s3Object.setKey(FILE_KEY);

        BucketOriginDetail bucketOriginDetail = new BucketOriginDetail();
        bucketOriginDetail.setName(bucketName.ssStageName());

        CreationDetail creationDetail = new CreationDetail();
        creationDetail.setObject(s3Object);
        creationDetail.setBucketOriginDetail(bucketOriginDetail);

        CreatedS3ObjectDto createdS3ObjectDto = new CreatedS3ObjectDto();
        createdS3ObjectDto.setCreationDetailObject(creationDetail);

        createdS3ObjectDto.setRetry(1);

        Acknowledgment acknowledgment = new Acknowledgment() {
            @Override
            public Future<?> acknowledge() {
                return null;
            }
        };
        putObjectInBucket(FILE_KEY, bucketName.ssStageName(), new byte[10]);
        mockGetDocument("application/pdf", AVAILABLE, List.of(DocumentType.TransformationsEnum.SIGN_AND_TIMEMARK));
        mockSignCalls();
        var testMono = transformationService.newStagingBucketObjectCreatedEvent(createdS3ObjectDto, acknowledgment);

        StepVerifier.create(testMono).expectNextCount(0).verifyComplete();
        verify(transformationService, times(1)).objectTransformation(anyString(), anyString(), anyInt(), anyBoolean());
    }

    @Test
    void newStagingBucketObjectCreatedEventRetryInHotBucketOk() {

        S3Object s3Object = new S3Object();
        s3Object.setKey(FILE_KEY);

        BucketOriginDetail bucketOriginDetail = new BucketOriginDetail();
        bucketOriginDetail.setName(bucketName.ssStageName());

        CreationDetail creationDetail = new CreationDetail();
        creationDetail.setObject(s3Object);
        creationDetail.setBucketOriginDetail(bucketOriginDetail);

        CreatedS3ObjectDto createdS3ObjectDto = new CreatedS3ObjectDto();
        createdS3ObjectDto.setCreationDetailObject(creationDetail);

        createdS3ObjectDto.setRetry(1);

        Acknowledgment acknowledgment = new Acknowledgment() {
            @Override
            public Future<?> acknowledge() {
                return null;
            }
        };

        putObjectInBucket(FILE_KEY, bucketName.ssHotName(), new byte[10]);
        mockGetDocument("application/pdf", AVAILABLE, List.of(DocumentType.TransformationsEnum.SIGN_AND_TIMEMARK));
        mockSignCalls();
        var testMono = transformationService.newStagingBucketObjectCreatedEvent(createdS3ObjectDto, acknowledgment);

        StepVerifier.create(testMono).expectNextCount(0).verifyComplete();
        verify(transformationService, times(1)).objectTransformation(anyString(), anyString(), anyInt(), anyBoolean());
    }

    void mockGetDocument(String contentType, String documentState, List<DocumentType.TransformationsEnum> transformations) {
        var documentType1 = new DocumentType().statuses(Map.ofEntries(Map.entry(PRELOADED, new CurrentStatus())))
                .tipoDocumento(DocTypesConstant.PN_AAR)
                .checksum(DocumentType.ChecksumEnum.MD5);
        var document = new Document().documentType(documentType1);
        document.setContentType(contentType);
        document.getDocumentType().setTransformations(transformations);
        document.setDocumentState(documentState);
        var documentResponse = new DocumentResponse().document(document);

        when(documentClientCall.getDocument(anyString())).thenReturn(Mono.just(documentResponse));
        when(documentClientCall.patchDocument(anyString(), anyString(), anyString(), any(DocumentChanges.class))).thenReturn(Mono.just(documentResponse));
    }

    void mockSignCalls() {
        SignReturnV2 signReturnV2 = new SignReturnV2();
        PnSignDocumentResponse pnSignDocumentResponse = new PnSignDocumentResponse();
        pnSignDocumentResponse.setSignedDocument(new byte[10]);

        when(arubaSignProviderService.signPdfDocument(any(), anyBoolean())).thenReturn(Mono.just(pnSignDocumentResponse));
        when(alternativeSignProviderService.signPdfDocument(any(), anyBoolean())).thenReturn(Mono.just(pnSignDocumentResponse));

        when(arubaSignProviderService.signXmlDocument(any(), anyBoolean())).thenReturn(Mono.just(pnSignDocumentResponse));
        when(alternativeSignProviderService.signXmlDocument(any(), anyBoolean())).thenReturn(Mono.just(pnSignDocumentResponse));

        when(arubaSignProviderService.pkcs7Signature(any(), anyBoolean())).thenReturn(Mono.just(pnSignDocumentResponse));
        when(alternativeSignProviderService.pkcs7Signature(any(), anyBoolean())).thenReturn(Mono.just(pnSignDocumentResponse));
    }

    void verifyArubaProviderSignCalls(String contentType) {
        switch (contentType) {
            case "application/pdf" -> verify(arubaSignProviderService, times(1)).signPdfDocument(any(), anyBoolean());
            case "application/xml" -> verify(arubaSignProviderService, times(1)).signXmlDocument(any(), anyBoolean());
            case "other" -> verify(arubaSignProviderService, times(1)).pkcs7Signature(any(), anyBoolean());
        }
    }

    void verifyAlternativeProviderSignCalls(String contentType) {
        switch (contentType) {
            case "application/pdf" -> verify(alternativeSignProviderService, times(1)).signPdfDocument(any(), anyBoolean());
            case "application/xml" -> verify(alternativeSignProviderService, times(1)).signXmlDocument(any(), anyBoolean());
            case "other" -> verify(alternativeSignProviderService, times(1)).pkcs7Signature(any(), anyBoolean());
        }
    }

    private void putObjectInBucket(String key, String bucketName, byte[] fileBytes) {
        s3TestClient.putObject(PutObjectRequest.builder()
                        .key(key)
                        .bucket(bucketName)
                        .contentMD5(new String(Base64.encodeBase64(DigestUtils.md5(fileBytes))))
                        .expires(Instant.now().plus(Duration.ofDays(1))).build(),
                RequestBody.fromBytes(fileBytes));
    }

    private void deleteObjectInBucket(String key, String bucketName) {
        s3TestClient.deleteObject(DeleteObjectRequest.builder().bucket(bucketName).key(key).build());
    }

}