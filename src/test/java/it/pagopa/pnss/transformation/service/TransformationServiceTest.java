package it.pagopa.pnss.transformation.service;

import io.awspring.cloud.messaging.listener.Acknowledgment;
import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.*;
import it.pagopa.pnss.common.DocTypesConstant;
import it.pagopa.pnss.common.client.DocumentClientCall;
import it.pagopa.pnss.common.client.exception.ArubaSignException;
import it.pagopa.pnss.common.client.exception.ArubaSignExceptionLimitCall;
import it.pagopa.pnss.common.exception.IllegalTransformationException;
import it.pagopa.pnss.common.exception.InvalidStatusTransformationException;
import it.pagopa.pnss.configurationproperties.BucketName;
import it.pagopa.pnss.testutils.annotation.SpringBootTestWebEnv;
import it.pagopa.pnss.transformation.model.dto.BucketOriginDetail;
import it.pagopa.pnss.transformation.model.dto.CreatedS3ObjectDto;
import it.pagopa.pnss.transformation.model.dto.CreationDetail;
import it.pagopa.pnss.transformation.model.dto.S3Object;
import it.pagopa.pnss.transformation.rest.call.aruba.ArubaSignServiceCall;
import it.pagopa.pnss.transformation.service.impl.S3ServiceImpl;
import it.pagopa.pnss.transformation.wsdl.SignReturnV2;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.After;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.annotation.DirtiesContext;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.TemporalAmount;
import java.time.temporal.TemporalUnit;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

import static it.pagopa.pnss.common.constant.Constant.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@SpringBootTestWebEnv
public class TransformationServiceTest {

    @SpyBean
    private TransformationService transformationService;
    @MockBean
    private DocumentClientCall documentClientCall;
    @MockBean
    private ArubaSignServiceCall arubaSignServiceCall;
    @Autowired
    private BucketName bucketName;
    @Autowired
    private S3Client s3TestClient;

    private final String FILE_KEY = "FILE_KEY";

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

//    @Test
//    void newStagingBucketObjectCreatedInvalidStatus() {
//
//        S3Object s3Object = new S3Object();
//        s3Object.setKey(FILE_KEY);
//
//        BucketOriginDetail bucketOriginDetail = new BucketOriginDetail();
//        bucketOriginDetail.setName(bucketName.ssStageName());
//
//        CreationDetail creationDetail = new CreationDetail();
//        creationDetail.setObject(s3Object);
//        creationDetail.setBucketOriginDetail(bucketOriginDetail);
//
//        CreatedS3ObjectDto createdS3ObjectDto = new CreatedS3ObjectDto();
//        createdS3ObjectDto.setCreationDetailObject(creationDetail);
//
//        Acknowledgment acknowledgment = new Acknowledgment() {
//            @Override
//            public Future<?> acknowledge() {
//                return null;
//            }
//        };
//
//        putObjectInBucket(FILE_KEY, bucketName.ssStageName(), new byte[10]);
//        mockGetDocument("application/pdf", AVAILABLE, List.of(DocumentType.TransformationsEnum.SIGN_AND_TIMEMARK));
//
//        var testMono = transformationService.newStagingBucketObjectCreatedEvent(createdS3ObjectDto, acknowledgment);
//
//        StepVerifier.create(testMono).expectError(InvalidStatusTransformationException.class).verify();
//        verify(transformationService, times(1)).objectTransformation(anyString(), anyString(), anyBoolean());
//    }

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

        putObjectInBucket(FILE_KEY, bucketName.ssStageName(), new byte[10]);
        mockGetDocument("application/pdf", STAGED, List.of(DocumentType.TransformationsEnum.NONE));

        var testMono = transformationService.newStagingBucketObjectCreatedEvent(createdS3ObjectDto, acknowledgment);

        StepVerifier.create(testMono).expectError(IllegalTransformationException.class).verify();
        verify(transformationService, times(1)).objectTransformation(anyString(), anyString(), anyBoolean());
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

        putObjectInBucket(FILE_KEY, bucketName.ssStageName(), new byte[10]);
        mockGetDocument("application/pdf", STAGED, List.of(DocumentType.TransformationsEnum.SIGN_AND_TIMEMARK));
        when(arubaSignServiceCall.signPdfDocument(any(), anyBoolean())).thenReturn(Mono.error(new ArubaSignException()));

        var testMono = transformationService.newStagingBucketObjectCreatedEvent(createdS3ObjectDto, acknowledgment);

        StepVerifier.create(testMono).expectError(ArubaSignException.class).verify();
    }

    @ParameterizedTest
    @ValueSource(strings = {"application/pdf", "application/xml", "other"})
    void newStagingBucketObjectCreatedEventOk(String contentType){

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

        putObjectInBucket(FILE_KEY, bucketName.ssStageName(), new byte[10]);
        mockGetDocument(contentType, STAGED, List.of(DocumentType.TransformationsEnum.SIGN_AND_TIMEMARK));
        mockArubaCalls();

        var testMono = transformationService.newStagingBucketObjectCreatedEvent(createdS3ObjectDto, acknowledgment);

        StepVerifier.create(testMono).expectNextCount(0).verifyComplete();
        verify(transformationService, times(1)).objectTransformation(anyString(), anyString(), anyBoolean());
    }

    @ParameterizedTest
    @ValueSource(strings = {"application/pdf", "application/xml", "other"})
    void newStagingBucketObjectCreatedEventAlreadyPresentInHotBucketOk(String contentType){

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

        putObjectInBucket(FILE_KEY, bucketName.ssHotName(), new byte[10]);
        mockGetDocument(contentType, STAGED, List.of(DocumentType.TransformationsEnum.SIGN_AND_TIMEMARK));
        mockArubaCalls();

        var testMono = transformationService.newStagingBucketObjectCreatedEvent(createdS3ObjectDto, acknowledgment);

        StepVerifier.create(testMono).expectNextCount(0).verifyComplete();
        verify(transformationService, times(1)).objectTransformation(anyString(), anyString(), anyBoolean());
    }

    void mockGetDocument(String contentType, String documentState, List<DocumentType.TransformationsEnum> transformations){
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

    void mockArubaCalls(){
        SignReturnV2 signReturnV2 = new SignReturnV2();
        signReturnV2.setDescription("test");
        signReturnV2.setReturnCode("ok");
        signReturnV2.setStatus("ok");
        signReturnV2.setBinaryoutput(new byte[10]);
        when(arubaSignServiceCall.signPdfDocument(any(), anyBoolean())).thenReturn(Mono.just(signReturnV2));

        when(arubaSignServiceCall.xmlSignature(any(), anyBoolean())).thenReturn(Mono.just(signReturnV2));

        when(arubaSignServiceCall.pkcs7signV2(any(), anyBoolean())).thenReturn(Mono.just(signReturnV2));
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
