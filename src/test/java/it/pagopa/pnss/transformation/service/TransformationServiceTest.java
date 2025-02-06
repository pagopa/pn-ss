package it.pagopa.pnss.transformation.service;

import it.pagopa.pn.library.exceptions.PnSpapiPermanentErrorException;
import it.pagopa.pn.library.sign.pojo.PnSignDocumentResponse;
import it.pagopa.pn.library.sign.service.impl.PnSignProviderService;
import it.pagopa.pnss.configurationproperties.BucketName;
import it.pagopa.pnss.testutils.annotation.SpringBootTestWebEnv;
import lombok.CustomLog;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.mock.mockito.SpyBean;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.TransformationMessage;

import java.time.Duration;
import java.time.Instant;
import java.util.stream.Stream;

import static it.pagopa.pnss.configurationproperties.TransformationProperties.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@SpringBootTestWebEnv
@CustomLog
class TransformationServiceTest {

    @SpyBean
    private TransformationService transformationService;
    @Autowired
    private BucketName bucketName;
    @Autowired
    private S3Client s3TestClient;
    @SpyBean
    private S3Service s3Service;
    @Value("${s3.queue.sign-queue-name}")
    private String signQueueName;
    @SpyBean
    private PnSignProviderService pnSignProviderService;
    @Value("${pn.ss.transformation.dummy-delay}")
    private Integer dummyDelay;
    private final String FILE_KEY = "FILE_KEY";
    private static final String SIGN_AND_TIMEMARK = "SIGN_AND_TIMEMARK";
    private static final String SIGN = "SIGN";
    private static final String DUMMY = "DUMMY";

    @BeforeEach
    void initialize() {
        putObjectInBucket(FILE_KEY, bucketName.ssStageName(), new byte[10]);
    }

    @AfterEach
    void clean() {
        deleteObjectInBucket(FILE_KEY, bucketName.ssHotName());
    }

    @ParameterizedTest
    @ValueSource(strings = {"application/pdf", "application/xml", "other"})
    void signAndTimemark_Ok(String contentType) {
        //GIVEN
        String bucket = bucketName.ssStageName();
        Tag tag = Tag.builder().key(TRANSFORMATION_TAG_PREFIX + SIGN_AND_TIMEMARK).value(OK).build();
        Tagging expectedTagging = Tagging.builder().tagSet(tag).build();

        //WHEN
        mockSignCalls();
        var testMono = transformationService.signAndTimemarkTransformation(createTransformationMessage(SIGN_AND_TIMEMARK, bucket, contentType), true);

        //THEN
        StepVerifier.create(testMono).expectNextMatches(PutObjectResponse.class::isInstance).verifyComplete();
        verify(s3Service).getObject(FILE_KEY, bucket);
        verifyPnSignProviderCalls(contentType, true);
        verify(s3Service).putObject(eq(FILE_KEY), any(), eq(contentType), eq(bucket), eq(expectedTagging));
    }

    @ParameterizedTest
    @ValueSource(strings = {"application/pdf", "application/xml", "other"})
    void sign_Ok(String contentType) {
        //GIVEN
        String bucket = bucketName.ssStageName();
        Tag tag = Tag.builder().key(TRANSFORMATION_TAG_PREFIX + SIGN).value(OK).build();
        Tagging expectedTagging = Tagging.builder().tagSet(tag).build();

        //WHEN
        mockSignCalls();
        var testMono = transformationService.signAndTimemarkTransformation(createTransformationMessage(SIGN, bucket, contentType), false);

        //THEN
        StepVerifier.create(testMono).expectNextMatches(PutObjectResponse.class::isInstance).verifyComplete();
        verify(s3Service).getObject(FILE_KEY, bucket);
        verifyPnSignProviderCalls(contentType, false);
        verify(s3Service).putObject(eq(FILE_KEY), any(), eq(contentType), eq(bucket), eq(expectedTagging));
    }

    @ParameterizedTest
    @MethodSource("provideSignAndTimemarkArgs")
    void signAndTimemark_Idempotence_Ok(String transformationType, boolean marcatura) {
        //GIVEN
        String contentType = "application/pdf";
        String bucket = bucketName.ssStageName();
        Tag tag = Tag.builder().key(TRANSFORMATION_TAG_PREFIX + transformationType).value(OK).build();
        s3TestClient.putObjectTagging(builder -> builder.tagging(Tagging.builder().tagSet(tag).build()).key(FILE_KEY).bucket(bucket));

        //WHEN
        mockSignCalls();
        var testMono = transformationService.signAndTimemarkTransformation(createTransformationMessage(transformationType, bucket, contentType), marcatura);

        //THEN
        StepVerifier.create(testMono).verifyComplete();
        verify(s3Service).getObject(FILE_KEY, bucket);
        verifyPnSignProviderCalls(contentType, marcatura);
        verify(s3Service, never()).putObject(anyString(), any(), anyString(), anyString(), any());
    }

    // Eccezione permanente. Ci aspettiamo che venga applicato il tag Tranformation-xxx=ERROR e che l'operazione dia segnale di completamento.
    @ParameterizedTest
    @MethodSource("provideSignAndTimemarkArgs")
    void signAndTimemark_SignProvider_Ko(String transformationType, boolean marcatura) {
        //GIVEN
        String contentType = "application/pdf";
        String bucket = bucketName.ssStageName();
        Tag tag = Tag.builder().key(TRANSFORMATION_TAG_PREFIX + transformationType).value(ERROR).build();
        Tagging expectedTagging = Tagging.builder().tagSet(tag).build();

        //WHEN
        when(pnSignProviderService.signPdfDocument(any(), any())).thenReturn(Mono.error(new PnSpapiPermanentErrorException("Permanent exception")));
        var testMono = transformationService.signAndTimemarkTransformation(createTransformationMessage(transformationType, bucket, contentType), marcatura);

        //THEN
        StepVerifier.create(testMono).verifyComplete();
        verify(s3Service).getObject(FILE_KEY, bucket);
        verify(s3Service).putObjectTagging(FILE_KEY, bucket, expectedTagging);
        verify(s3Service, never()).putObject(anyString(), any(), anyString(), anyString(), any());
    }

    @ParameterizedTest
    @MethodSource("provideSignAndTimemarkArgs")
    void signAndTimemark_S3_Ko(String transformationType, boolean marcatura) {
        //GIVEN
        String bucket = bucketName.ssStageName();
        String contentType = "application/pdf";

        //WHEN
        mockSignCalls();
        var testMono = transformationService.signAndTimemarkTransformation(createTransformationMessage(transformationType, bucket, contentType, "FAKE"), marcatura);

        //THEN
        StepVerifier.create(testMono).expectError(NoSuchKeyException.class).verify();
    }

    @Test
    void dummy_Ok() {
        //GIVEN
        String bucket = bucketName.ssStageName();
        Tag tag = Tag.builder().key(TRANSFORMATION_TAG_PREFIX + DUMMY).value(OK).build();
        Tagging expectedTagging = Tagging.builder().tagSet(tag).build();

        //WHEN
        var testMono = transformationService.dummyTransformation(createTransformationMessage(DUMMY, bucket, null));

        //THEN
        StepVerifier.create(testMono)
                .expectSubscription()
                .expectNoEvent(Duration.ofMillis(dummyDelay))
                .thenAwait(Duration.ofMillis(2))
                .expectNextMatches(PutObjectTaggingResponse.class::isInstance)
                .verifyComplete();
        verify(s3Service).putObjectTagging(FILE_KEY, bucket, expectedTagging);
    }

    @Test
    void dummy_Idempotence_Ok() {
        //GIVEN
        String bucket = bucketName.ssStageName();
        Tag tag = Tag.builder().key(TRANSFORMATION_TAG_PREFIX + DUMMY).value(OK).build();
        s3TestClient.putObjectTagging(builder -> builder.tagging(Tagging.builder().tagSet(tag).build()).key(FILE_KEY).bucket(bucket));

        //WHEN
        var testMono = transformationService.dummyTransformation(createTransformationMessage(DUMMY, bucket, null));

        //THEN
        StepVerifier.create(testMono).verifyComplete();
        verify(s3Service, never()).putObjectTagging(anyString(), anyString(), any());
    }

    @Test
    void dummy_S3_Ko() {
        //GIVEN
        String bucket = bucketName.ssStageName();

        //WHEN
        var testMono = transformationService.dummyTransformation(createTransformationMessage(DUMMY, bucket, null, "FAKE"));

        //THEN
        StepVerifier.create(testMono).expectError(NoSuchKeyException.class).verify();
    }

    private TransformationMessage createTransformationMessage(String transformationType, String bucketName, String contentType, String fileKey) {
        TransformationMessage transformationMessage = new TransformationMessage();
        transformationMessage.setFileKey(fileKey);
        transformationMessage.setTransformationType(transformationType);
        transformationMessage.setBucketName(bucketName);
        transformationMessage.setContentType(contentType);
        return transformationMessage;
    }

    private TransformationMessage createTransformationMessage(String transformationType, String bucketName, String contentType) {
        return createTransformationMessage(transformationType, bucketName, contentType, FILE_KEY);
    }

    void mockSignCalls() {
        PnSignDocumentResponse pnSignDocumentResponse = new PnSignDocumentResponse();
        pnSignDocumentResponse.setSignedDocument(new byte[10]);
        when(pnSignProviderService.signPdfDocument(any(), anyBoolean())).thenReturn(Mono.just(pnSignDocumentResponse));
        when(pnSignProviderService.signXmlDocument(any(), anyBoolean())).thenReturn(Mono.just(pnSignDocumentResponse));
        when(pnSignProviderService.pkcs7Signature(any(), anyBoolean())).thenReturn(Mono.just(pnSignDocumentResponse));
    }

    void verifyPnSignProviderCalls(String contentType, boolean marcatura) {
        switch (contentType) {
            case "application/pdf" -> verify(pnSignProviderService, times(1)).signPdfDocument(any(), eq(marcatura));
            case "application/xml" -> verify(pnSignProviderService, times(1)).signXmlDocument(any(), eq(marcatura));
            case "other" -> verify(pnSignProviderService, times(1)).pkcs7Signature(any(), eq(marcatura));
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

    private static Stream<Arguments> provideSignAndTimemarkArgs() {
        return Stream.of(Arguments.of(SIGN_AND_TIMEMARK, true), Arguments.of(SIGN, false));
    }


}