package it.pagopa.pnss.transformation.service;

import io.awspring.cloud.messaging.listener.Acknowledgment;
import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.CurrentStatus;
import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.Document;
import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.DocumentResponse;
import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.DocumentType;
import it.pagopa.pnss.common.DocTypesConstant;
import it.pagopa.pnss.common.client.DocumentClientCall;
import it.pagopa.pnss.common.client.exception.ArubaSignException;
import it.pagopa.pnss.common.client.exception.ArubaSignExceptionLimitCall;
import it.pagopa.pnss.common.exception.IllegalTransformationException;
import it.pagopa.pnss.common.exception.InvalidStatusTransformationException;
import it.pagopa.pnss.testutils.annotation.SpringBootTestWebEnv;
import it.pagopa.pnss.transformation.model.dto.BucketOriginDetail;
import it.pagopa.pnss.transformation.model.dto.CreatedS3ObjectDto;
import it.pagopa.pnss.transformation.model.dto.CreationDetail;
import it.pagopa.pnss.transformation.model.dto.S3Object;
import it.pagopa.pnss.transformation.rest.call.aruba.ArubaSignServiceCall;
import it.pagopa.pnss.transformation.service.impl.S3ServiceImpl;
import it.pagopa.pnss.transformation.wsdl.SignReturnV2;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.services.s3.model.DeleteObjectResponse;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

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

    @MockBean
    S3ServiceImpl s3Service;

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
        s3Object.setKey("111-DDD");

        BucketOriginDetail bucketOriginDetail = new BucketOriginDetail();
        bucketOriginDetail.setName("prova");

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

        mockGetDocument("application/pdf", AVAILABLE, List.of(DocumentType.TransformationsEnum.SIGN_AND_TIMEMARK));

        var testMono = transformationService.newStagingBucketObjectCreatedEvent(createdS3ObjectDto, acknowledgment);

        StepVerifier.create(testMono).expectError(InvalidStatusTransformationException.class).verify();
        verify(transformationService, times(1)).objectTransformation(anyString(), anyString(), anyBoolean());
    }

    @Test
    void newStagingBucketObjectCreatedInvalidTransformation() {

        S3Object s3Object = new S3Object();
        s3Object.setKey("111-DDD");

        BucketOriginDetail bucketOriginDetail = new BucketOriginDetail();
        bucketOriginDetail.setName("prova");

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

        StepVerifier.create(testMono).expectError(IllegalTransformationException.class).verify();
        verify(transformationService, times(1)).objectTransformation(anyString(), anyString(), anyBoolean());
    }

    @Test
    void newStagingBucketObjectCreatedEventArubaKo() {

        S3Object s3Object = new S3Object();
        s3Object.setKey("111-DDD");

        BucketOriginDetail bucketOriginDetail = new BucketOriginDetail();
        bucketOriginDetail.setName("prova");

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
        when(s3Service.getObject(anyString(), anyString())).thenReturn(Mono.just(ResponseBytes.fromByteArray(GetObjectResponse.builder().build(), new byte[10])));
        when(arubaSignServiceCall.signPdfDocument(any(), anyBoolean())).thenReturn(Mono.error(new ArubaSignException()));

        var testMono = transformationService.newStagingBucketObjectCreatedEvent(createdS3ObjectDto, acknowledgment);

        StepVerifier.create(testMono).expectError(ArubaSignExceptionLimitCall.class).verify();
    }

    @ParameterizedTest
    @ValueSource(strings = {"application/pdf", "application/xml", "other"})
    void newStagingBucketObjectCreatedEventOk(String contentType){

        S3Object s3Object = new S3Object();
        s3Object.setKey("111-DDD");

        BucketOriginDetail bucketOriginDetail = new BucketOriginDetail();
        bucketOriginDetail.setName("prova");

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
        when(s3Service.getObject(anyString(), anyString())).thenReturn(Mono.just(ResponseBytes.fromByteArray(GetObjectResponse.builder().build(), new byte[10])));
        mockArubaCalls();

        when(s3Service.putObject(anyString(), any(), anyString())).thenReturn(Mono.just(PutObjectResponse.builder().build()));
        when(s3Service.deleteObject(anyString(), anyString())).thenReturn(Mono.just(DeleteObjectResponse.builder().build()));

        var testMono = transformationService.newStagingBucketObjectCreatedEvent(createdS3ObjectDto, acknowledgment);

        StepVerifier.create(testMono).expectNextCount(0).verifyComplete();
        verify(transformationService, times(1)).objectTransformation(anyString(), anyString(), anyBoolean());
    }

    void mockGetDocument(String contentType, String documentState, List<DocumentType.TransformationsEnum> transformations){
        var documentType1 = new DocumentType().statuses(Map.ofEntries(Map.entry(PRELOADED, new CurrentStatus()))).tipoDocumento(
                DocTypesConstant.PN_AAR);
        var document = new Document().documentType(documentType1);
        document.setContentType(contentType);
        document.getDocumentType().setTransformations(transformations);
        document.setDocumentState(documentState);
        var documentResponse = new DocumentResponse().document(document);

        when(documentClientCall.getDocument(anyString())).thenReturn(Mono.just(documentResponse));
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
}
