package it.pagopa.pnss.transformation.service;

import it.pagopa.pn.library.exceptions.PnSpapiPermanentErrorException;
import it.pagopa.pn.library.sign.configurationproperties.PnSignServiceConfigurationProperties;
import it.pagopa.pn.library.sign.pojo.PnSignDocumentResponse;;
import it.pagopa.pn.library.sign.service.impl.PnSignProviderService;
import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.*;
import it.pagopa.pnss.common.DocTypesConstant;
import it.pagopa.pnss.common.client.DocumentClientCall;
import it.pagopa.pnss.common.exception.SqsClientException;
import it.pagopa.pnss.common.service.EventBridgeService;
import it.pagopa.pnss.configurationproperties.AvailabelDocumentEventBridgeName;
import it.pagopa.pnss.transformation.exception.InvalidDocumentStateException;
import it.pagopa.pnss.transformation.exception.InvalidTransformationStateException;
import it.pagopa.pnss.common.service.SqsService;
import it.pagopa.pnss.configuration.TransformationConfig;
import it.pagopa.pnss.configurationproperties.BucketName;
import it.pagopa.pnss.testutils.annotation.SpringBootTestWebEnv;
import it.pagopa.pnss.transformation.model.dto.S3EventNotificationDetail;
import it.pagopa.pnss.transformation.model.dto.S3EventNotificationMessage;
import it.pagopa.pnss.transformation.model.dto.S3Object;
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
import org.springframework.boot.test.mock.mockito.MockBean;
import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.TransformationMessage;
import org.springframework.boot.test.mock.mockito.SpyBean;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import java.util.stream.Stream;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static it.pagopa.pnss.configurationproperties.TransformationProperties.*;


import static it.pagopa.pnss.common.constant.Constant.*;
import static it.pagopa.pnss.transformation.service.TransformationService.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@SpringBootTestWebEnv
@CustomLog
class TransformationServiceTest {

    @SpyBean
    private TransformationService transformationService;
    @MockBean
    private DocumentClientCall documentClientCall;
    @Autowired
    private BucketName bucketName;
    @Autowired
    private S3Client s3TestClient;
    @Autowired
    private PnSignServiceConfigurationProperties pnSignServiceConfigurationProperties;
    @Autowired
    AvailabelDocumentEventBridgeName availabelDocumentEventBridgeName;
    @SpyBean
    private SqsService sqsService;
    @SpyBean
    private S3Service s3Service;
    @Autowired
    private SqsAsyncClient sqsAsyncClient;
    @SpyBean
    private TransformationConfig transformationConfig;
    @SpyBean
    private EventBridgeService eventBridgeService;
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
        deleteObjectInBucket(FILE_KEY, bucketName.ssStageName());
    }

    @Test
    void handleEvent_FirstTransformation_Ok() {
        //GIVEN
        String sourceBucket = bucketName.ssStageName();
        String contentType = "application/pdf";
        String nextTransformation = SIGN_AND_TIMEMARK;
        S3EventNotificationMessage record = createS3Event(OBJECT_CREATED_PUT_EVENT);
        TransformationMessage expectedMessage = createTransformationMessage(nextTransformation, sourceBucket, contentType);

        //WHEN
        mockGetDocument(contentType, STAGED, List.of(nextTransformation));
        var testMono = transformationService.handleS3Event(record);

        //THEN
        StepVerifier.create(testMono).verifyComplete();
        verify(sqsService).send(any(), eq(expectedMessage));
    }

    @Test
    void handleEvent_FirstTransformation_ObjectHasMoreVersions_Ok() {
        //GIVEN
        String sourceBucket = bucketName.ssStageName();
        String contentType = "application/pdf";
        String nextTransformation = SIGN_AND_TIMEMARK;
        S3EventNotificationMessage record = createS3Event(OBJECT_CREATED_PUT_EVENT);
        TransformationMessage expectedMessage = createTransformationMessage(nextTransformation, sourceBucket, contentType);

        //WHEN
        mockGetDocument(contentType, STAGED, List.of(nextTransformation));
        var testMono = transformationService.handleS3Event(record);

        //THEN
        StepVerifier.create(testMono).verifyComplete();
        verify(sqsService).send(any(), eq(expectedMessage));
    }

    @Test
    void handleEvent_OtherTransformations_Ok() {
        //GIVEN
        String sourceBucket = bucketName.ssStageName();
        String contentType = "application/pdf";
        String nextTransformation = DUMMY;
        S3EventNotificationMessage record = createS3Event(OBJECT_TAGGING_PUT_EVENT);
        TransformationMessage expectedMessage = createTransformationMessage(nextTransformation, sourceBucket, contentType);

        // Simuliamo il fatto che la prima trasformazione è già stata fatta.
        Tag tag = Tag.builder().key("Transformation-" + SIGN_AND_TIMEMARK).value("OK").build();
        s3TestClient.putObjectTagging(builder -> builder.key(FILE_KEY).bucket(sourceBucket).tagging(Tagging.builder().tagSet(tag).build()));

        //WHEN
        mockGetDocument(contentType, STAGED, List.of(SIGN_AND_TIMEMARK, nextTransformation));
        var testMono = transformationService.handleS3Event(record);

        //THEN
        StepVerifier.create(testMono).verifyComplete();
        verify(s3Service).getObjectTagging(FILE_KEY, sourceBucket);
        verify(sqsService).send(any(), eq(expectedMessage));
    }

    // Il valore associato al tag Transformation è "ERROR". Ci aspettiamo che venga notificato un evento di indisponibilità.
    @Test
    void handleEvent_OtherTransformations_ErrorState() {
        //GIVEN
        String sourceBucket = bucketName.ssStageName();
        String contentType = "application/pdf";
        S3EventNotificationMessage record = createS3Event(OBJECT_TAGGING_PUT_EVENT);

        // Simuliamo il fatto che la prima trasformazione è già stata fatta.
        Tag tag = Tag.builder().key("Transformation-" + SIGN_AND_TIMEMARK).value("ERROR").build();
        s3TestClient.putObjectTagging(builder -> builder.key(FILE_KEY).bucket(sourceBucket).tagging(Tagging.builder().tagSet(tag).build()));

        //WHEN
        mockGetDocument(contentType, STAGED, List.of(SIGN_AND_TIMEMARK, DUMMY));
        var testMono = transformationService.handleS3Event(record);

        //THEN
        StepVerifier.create(testMono).verifyComplete();
        verify(s3Service).getObjectTagging(FILE_KEY, sourceBucket);
        verify(eventBridgeService).putSingleEvent(any());
    }

    // L'oggetto S3 ha un tag di tipo Transformation-xxx, ma il valore associato non è riconosciuto.
    @Test
    void handleEvent_OtherTransformations_UnrecognizedTransformationState_Ko() {
        //GIVEN
        String sourceBucket = bucketName.ssStageName();
        String contentType = "application/pdf";
        String nextTransformation = DUMMY;
        S3EventNotificationMessage record = createS3Event(OBJECT_TAGGING_PUT_EVENT);
        TransformationMessage expectedMessage = createTransformationMessage(nextTransformation, sourceBucket, contentType);

        // Inseriamo un tag Transformation, ma con uno stato non riconosciuto.
        Tag tag = Tag.builder().key("Transformation-" + SIGN_AND_TIMEMARK).value("FAKE").build();
        s3TestClient.putObjectTagging(builder -> builder.key(FILE_KEY).bucket(sourceBucket).tagging(Tagging.builder().tagSet(tag).build()));

        //WHEN
        mockGetDocument(contentType, STAGED, List.of(SIGN_AND_TIMEMARK, nextTransformation));
        var testMono = transformationService.handleS3Event(record);

        //THEN
        StepVerifier.create(testMono).expectError(InvalidTransformationStateException.StatusNotRecognizedException.class).verify();
        verify(s3Service).getObjectTagging(FILE_KEY, sourceBucket);
    }


    @Test
    void handleEvent_AllTransformationsApplied_Ok() {
        //GIVEN
        String sourceBucket = bucketName.ssStageName();
        String contentType = "application/pdf";
        String lastTransformation = DUMMY;
        S3EventNotificationMessage record = createS3Event(OBJECT_TAGGING_PUT_EVENT);

        // Simuliamo il fatto che l'ultima trasformazione è stata applicata.
        Tag tag = Tag.builder().key("Transformation-" + lastTransformation).value("OK").build();
        s3TestClient.putObjectTagging(builder -> builder.key(FILE_KEY).bucket(sourceBucket).tagging(Tagging.builder().tagSet(tag).build()));

        //WHEN
        mockGetDocument(contentType, STAGED, List.of(SIGN_AND_TIMEMARK, lastTransformation));
        var testMono = transformationService.handleS3Event(record);

        //THEN
        StepVerifier.create(testMono).verifyComplete();
        verify(s3Service).getObject(FILE_KEY, sourceBucket);
        verify(s3Service).putObject(eq(FILE_KEY), any(), eq(contentType), eq(bucketName.ssHotName()));
    }

    // Tutte le trasformazioni sono applicate, ma il file è già presente nel bucket finale.
    // Controlliamo che il meccanismo di idempotenza funzioni (il file non deve essere caricato di nuovo).
    @Test
    void handleEvent_AllTransformationsApplied_Idempotence_Ok() {
        //GIVEN
        String sourceBucket = bucketName.ssStageName();
        String contentType = "application/pdf";
        String lastTransformation = DUMMY;
        S3EventNotificationMessage record = createS3Event(OBJECT_TAGGING_PUT_EVENT);

        // Simuliamo che l'ultima trasformazione è stata applicata.
        Tag tag = Tag.builder().key("Transformation-" + lastTransformation).value("OK").build();
        s3TestClient.putObjectTagging(builder -> builder.key(FILE_KEY).bucket(sourceBucket).tagging(Tagging.builder().tagSet(tag).build()));
        // Simuliamo che il file è già presente nel bucket finale
        putObjectInBucket(FILE_KEY, bucketName.ssHotName(),  new byte[10]);

        //WHEN
        mockGetDocument(contentType, STAGED, List.of(SIGN_AND_TIMEMARK, lastTransformation));
        var testMono = transformationService.handleS3Event(record);

        //THEN
        StepVerifier.create(testMono).verifyComplete();
        verify(s3Service, never()).getObject(anyString(), anyString());
        verify(s3Service, never()).putObject(anyString(), any(), anyString(), anyString());
    }

    @Test
    void handleEvent_InvalidTransformation_Ko() {
        //GIVEN
        String contentType = "application/pdf";
        S3EventNotificationMessage record = createS3Event(OBJECT_CREATED_PUT_EVENT);

        //WHEN
        mockGetDocument(contentType, STAGED, List.of("NONE"));
        var testMono = transformationService.handleS3Event(record);

        //THEN
        StepVerifier.create(testMono).expectError(IllegalArgumentException.class).verify();
    }

    @Test
    void handleEvent_S3_Ko() {
        //GIVEN
        String contentType = "application/pdf";
        S3EventNotificationMessage record = createS3Event(OBJECT_CREATED_PUT_EVENT, "FAKE");

        //WHEN
        mockGetDocument(contentType, STAGED, List.of(SIGN_AND_TIMEMARK));
        var testMono = transformationService.handleS3Event(record);

        //THEN
        StepVerifier.create(testMono).expectError(NoSuchKeyException.class).verify();
    }

    @Test
    void handleEvent_Sqs_Ko() {
        //GIVEN
        String sourceBucket = bucketName.ssStageName();
        String contentType = "application/pdf";
        String nextTransformation = SIGN_AND_TIMEMARK;
        S3EventNotificationMessage record = createS3Event(OBJECT_CREATED_PUT_EVENT);
        TransformationMessage expectedMessage = createTransformationMessage(nextTransformation, sourceBucket, contentType);

        //WHEN
        mockGetDocument(contentType, STAGED, List.of(nextTransformation));
        when(transformationConfig.getTransformationQueueName(SIGN_AND_TIMEMARK)).thenReturn("fake-queue");
        var testMono = transformationService.handleS3Event(record);

        //THEN
        StepVerifier.create(testMono).expectError(SqsClientException.class).verify();
        verify(sqsService).send(any(), eq(expectedMessage));
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
        it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.TransformationMessage transformationMessage = new it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.TransformationMessage();
        transformationMessage.setFileKey(fileKey);
        transformationMessage.setTransformationType(transformationType);
        transformationMessage.setBucketName(bucketName);
        transformationMessage.setContentType(contentType);
        return transformationMessage;
    }

    private TransformationMessage createTransformationMessage(String transformationType, String bucketName, String contentType) {
        return createTransformationMessage(transformationType, bucketName, contentType, FILE_KEY);
    }

    void mockGetDocument(String contentType, String documentState, List<String> transformations) {
        var documentType1 = new DocumentType().statuses(Map.ofEntries(Map.entry(PRELOADED, new CurrentStatus())))
                .tipoDocumento(DocTypesConstant.PN_AAR)
                .checksum(DocumentType.ChecksumEnum.MD5);
        var document = new Document().documentType(documentType1);
        document.setDocumentKey(FILE_KEY);
        document.setContentType(contentType);
        document.getDocumentType().setTransformations(transformations);
        document.setDocumentState(documentState);
        var documentResponse = new DocumentResponse().document(document);

        when(documentClientCall.getDocument(anyString())).thenReturn(Mono.just(documentResponse));
        when(documentClientCall.patchDocument(anyString(), anyString(), anyString(), any(DocumentChanges.class))).thenReturn(Mono.just(documentResponse));
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
        ListObjectVersionsResponse response = s3TestClient.listObjectVersions(ListObjectVersionsRequest.builder().prefix(key).bucket(bucketName).build());
        response.versions().forEach(version -> s3TestClient.deleteObject(DeleteObjectRequest.builder().bucket(bucketName).key(version.key()).versionId(version.versionId()).build()));
        response.deleteMarkers().forEach(deleteMarker -> s3TestClient.deleteObject(DeleteObjectRequest.builder().bucket(bucketName).key(deleteMarker.key()).versionId(deleteMarker.versionId()).build()));
    }

    private S3EventNotificationMessage createS3Event(String eventName, String fileKey) {
        S3Object s3Object = S3Object.builder().key(fileKey).build();
        S3EventNotificationDetail detail = S3EventNotificationDetail.builder()
                .object(s3Object)
                .reason(eventName.equals(OBJECT_CREATED_PUT_EVENT) ? PUT_OBJECT_REASON : null)
                .build();
        return S3EventNotificationMessage.builder().detailType(eventName).eventNotificationDetail(detail).build();
    }

    private S3EventNotificationMessage createS3Event(String eventName) {
        return createS3Event(eventName, FILE_KEY);
    }

    private static Stream<Arguments> provideSignAndTimemarkArgs() {
        return Stream.of(Arguments.of(SIGN_AND_TIMEMARK, true), Arguments.of(SIGN, false));
    }


}