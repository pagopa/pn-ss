package it.pagopa.pnss.transformation.service;

import com.namirial.sign.library.service.PnSignServiceImpl;
import it.pagopa.pn.library.sign.configurationproperties.PnSignServiceConfigurationProperties;
import it.pagopa.pn.library.sign.pojo.PnSignDocumentResponse;
import it.pagopa.pn.library.sign.service.impl.ArubaSignProviderService;
import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.*;
import it.pagopa.pnss.common.DocTypesConstant;
import it.pagopa.pnss.common.client.DocumentClientCall;
import it.pagopa.pnss.common.exception.SqsClientException;
import it.pagopa.pnss.common.service.EventBridgeService;
import it.pagopa.pnss.transformation.exception.InvalidDocumentStateException;
import it.pagopa.pnss.transformation.exception.InvalidTransformationStateException;
import it.pagopa.pnss.common.rest.call.pdfraster.PdfRasterCall;
import it.pagopa.pnss.common.service.SqsService;
import it.pagopa.pnss.configuration.TransformationConfig;
import it.pagopa.pnss.configurationproperties.BucketName;
import it.pagopa.pnss.testutils.annotation.SpringBootTestWebEnv;
import it.pagopa.pnss.transformation.model.dto.TransformationMessage;
import lombok.CustomLog;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.eventnotifications.s3.model.S3;
import software.amazon.awssdk.eventnotifications.s3.model.S3Bucket;
import software.amazon.awssdk.eventnotifications.s3.model.S3EventNotificationRecord;
import software.amazon.awssdk.eventnotifications.s3.model.S3Object;
import software.amazon.awssdk.eventnotifications.s3.model.UserIdentity;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static it.pagopa.pnss.common.constant.Constant.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@SpringBootTestWebEnv
@CustomLog
class TransformationServiceTest {
/*
    @SpyBean
    private TransformationService transformationService;
    @MockBean
    private DocumentClientCall documentClientCall;
    @MockBean
    private ArubaSignProviderService arubaSignProviderService;
    @MockBean
    private PnSignServiceImpl namirialProviderService;
    @Autowired
    private BucketName bucketName;
    @Autowired
    private S3Client s3TestClient;
    @Autowired
    private PnSignServiceConfigurationProperties pnSignServiceConfigurationProperties;
    @SpyBean
    private SqsService sqsService;
    @SpyBean
    private S3Service s3Service;
    @MockBean
    private PdfRasterCall pdfRasterCall;
    @Autowired
    private SqsAsyncClient sqsAsyncClient;
    @SpyBean
    private TransformationConfig transformationConfig;
    @SpyBean
    private EventBridgeService eventBridgeService;
    private final String FILE_KEY = "FILE_KEY";
    private static final String SIGN_AND_TIMEMARK = "SIGN_AND_TIMEMARK";
    private static final String SIGN = "SIGN";
    private static final String DUMMY = "DUMMY";
    private final String OBJECT_CREATED_PUT = "ObjectCreated:Put";
    private final String OBJECT_TAGGING_PUT = "ObjectTagging:Put";

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
        S3EventNotificationRecord record = createS3Event(OBJECT_CREATED_PUT);
        TransformationMessage expectedMessage = TransformationMessage.builder()
                .fileKey(FILE_KEY)
                .bucketName(sourceBucket)
                .contentType(contentType)
                .transformationType(nextTransformation)
                .build();

        //WHEN
        mockGetDocument(contentType, STAGED, List.of(nextTransformation));
        var testMono = transformationService.handleS3Event(record);

        //THEN
        StepVerifier.create(testMono).verifyComplete();
        verify(s3Service).listObjectVersions(FILE_KEY, sourceBucket);
        verify(sqsService).send(any(), eq(expectedMessage));
    }

    @Test
    void handleEvent_FirstTransformation_ObjectHasMoreVersions_Ok() {
        //GIVEN
        String sourceBucket = bucketName.ssStageName();
        String contentType = "application/pdf";
        String nextTransformation = SIGN_AND_TIMEMARK;
        S3EventNotificationRecord record = createS3Event(OBJECT_CREATED_PUT);
        TransformationMessage expectedMessage = TransformationMessage.builder()
                .fileKey(FILE_KEY)
                .bucketName(sourceBucket)
                .contentType(contentType)
                .transformationType(nextTransformation)
                .build();

        //WHEN
        mockGetDocument(contentType, STAGED, List.of(nextTransformation));
        var testMono = transformationService.handleS3Event(record);

        //THEN
        StepVerifier.create(testMono).verifyComplete();
        verify(s3Service).listObjectVersions(FILE_KEY, sourceBucket);
        verify(sqsService).send(any(), eq(expectedMessage));
    }

    @Test
    void handleEvent_OtherTransformations_Ok() {
        //GIVEN
        String sourceBucket = bucketName.ssStageName();
        String contentType = "application/pdf";
        String nextTransformation = DUMMY;
        S3EventNotificationRecord record = createS3Event(OBJECT_TAGGING_PUT);
        TransformationMessage expectedMessage = TransformationMessage.builder()
                .fileKey(FILE_KEY)
                .bucketName(sourceBucket)
                .contentType(contentType)
                .transformationType(nextTransformation)
                .build();

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
        String nextTransformation = DUMMY;
        S3EventNotificationRecord record = createS3Event(OBJECT_TAGGING_PUT);
        TransformationMessage expectedMessage = TransformationMessage.builder()
                .fileKey(FILE_KEY)
                .bucketName(sourceBucket)
                .contentType(contentType)
                .transformationType(nextTransformation)
                .build();

        // Simuliamo il fatto che la prima trasformazione è già stata fatta.
        Tag tag = Tag.builder().key("Transformation-" + SIGN_AND_TIMEMARK).value("ERROR").build();
        s3TestClient.putObjectTagging(builder -> builder.key(FILE_KEY).bucket(sourceBucket).tagging(Tagging.builder().tagSet(tag).build()));

        //WHEN
        mockGetDocument(contentType, STAGED, List.of(SIGN_AND_TIMEMARK, nextTransformation));
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
        S3EventNotificationRecord record = createS3Event(OBJECT_TAGGING_PUT);
        TransformationMessage expectedMessage = TransformationMessage.builder()
                .fileKey(FILE_KEY)
                .bucketName(sourceBucket)
                .contentType(contentType)
                .transformationType(nextTransformation)
                .build();

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

    // L'oggetto S3 non ha un tag di tipo Transformation-xxx, quindi lo stato della trasformazione non è considerato valido.
    @Test
    void handleEvent_OtherTransformations_TransformationStateNotFound_Ko() {
        //GIVEN
        String sourceBucket = bucketName.ssStageName();
        String contentType = "application/pdf";
        S3EventNotificationRecord record = createS3Event(OBJECT_TAGGING_PUT);

        // Inseriamo un tag che non ci aspettiamo. Il tag di tipo Transformation-xxx non viene inserito.
        Tag tag = Tag.builder().key("FakeTagKey").value("FakeTagValue").build();
        s3TestClient.putObjectTagging(builder -> builder.key(FILE_KEY).bucket(sourceBucket).tagging(Tagging.builder().tagSet(tag).build()));

        //WHEN
        mockGetDocument(contentType, STAGED, List.of(SIGN_AND_TIMEMARK, DUMMY));
        var testMono = transformationService.handleS3Event(record);

        //THEN
        StepVerifier.create(testMono).expectError(InvalidTransformationStateException.StatusNotFoundException.class).verify();
    }

    @Test
    void handleEvent_AllTransformationsApplied_Ok() {
        //GIVEN
        String sourceBucket = bucketName.ssStageName();
        String contentType = "application/pdf";
        String lastTransformation = DUMMY;
        S3EventNotificationRecord record = createS3Event(OBJECT_TAGGING_PUT);

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
        S3EventNotificationRecord record = createS3Event(OBJECT_TAGGING_PUT);

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
    void handleEvent_InvalidDocumentState_Ko() {
        //GIVEN
        String contentType = "application/pdf";
        S3EventNotificationRecord record = createS3Event(OBJECT_CREATED_PUT);

        //WHEN
        mockGetDocument(contentType, ATTACHED, List.of(SIGN_AND_TIMEMARK));
        var testMono = transformationService.handleS3Event(record);

        //THEN
        StepVerifier.create(testMono).expectError(InvalidDocumentStateException.class).verify();
    }

    @Test
    void handleEvent_InvalidTransformation_Ko() {
        //GIVEN
        String contentType = "application/pdf";
        S3EventNotificationRecord record = createS3Event(OBJECT_CREATED_PUT);

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
        S3EventNotificationRecord record = createS3Event(OBJECT_CREATED_PUT, "FAKE");

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
        S3EventNotificationRecord record = createS3Event(OBJECT_CREATED_PUT);
        TransformationMessage expectedMessage = TransformationMessage.builder()
                .fileKey(FILE_KEY)
                .bucketName(sourceBucket)
                .contentType(contentType)
                .transformationType(nextTransformation)
                .build();

        //WHEN
        mockGetDocument(contentType, STAGED, List.of(nextTransformation));
        when(transformationConfig.getTransformationQueueName(SIGN_AND_TIMEMARK)).thenReturn("fake-queue");
        var testMono = transformationService.handleS3Event(record);

        //THEN
        StepVerifier.create(testMono).expectError(SqsClientException.class).verify();
        verify(s3Service).listObjectVersions(FILE_KEY, sourceBucket);
        verify(sqsService).send(any(), eq(expectedMessage));
    }

    void mockGetDocument(String contentType, String documentState, List<String> transformations) {
        var documentType1 = new DocumentType().statuses(Map.ofEntries(Map.entry(PRELOADED, new CurrentStatus())))
                .tipoDocumento(DocTypesConstant.PN_AAR)
                .checksum(DocumentType.ChecksumEnum.MD5);
        var document = new Document().documentType(documentType1);
        document.setDocumentKey(FILE_KEY);
        document.setContentType(contentType);
        document.getDocumentType().setTransformations(transformations.stream().map(DocumentType.TransformationsEnum::fromValue).toList());
        document.setDocumentState(documentState);
        var documentResponse = new DocumentResponse().document(document);

        when(documentClientCall.getDocument(anyString())).thenReturn(Mono.just(documentResponse));
        when(documentClientCall.patchDocument(anyString(), anyString(), anyString(), any(DocumentChanges.class))).thenReturn(Mono.just(documentResponse));
    }

    void mockSignCalls() {
        PnSignDocumentResponse pnSignDocumentResponse = new PnSignDocumentResponse();
        pnSignDocumentResponse.setSignedDocument(new byte[10]);

        when(arubaSignProviderService.signPdfDocument(any(), anyBoolean())).thenReturn(Mono.just(pnSignDocumentResponse));
        when(namirialProviderService.signPdfDocument(any(), anyBoolean())).thenReturn(Mono.just(pnSignDocumentResponse));

        when(arubaSignProviderService.signXmlDocument(any(), anyBoolean())).thenReturn(Mono.just(pnSignDocumentResponse));
        when(namirialProviderService.signXmlDocument(any(), anyBoolean())).thenReturn(Mono.just(pnSignDocumentResponse));

        when(arubaSignProviderService.pkcs7Signature(any(), anyBoolean())).thenReturn(Mono.just(pnSignDocumentResponse));
        when(namirialProviderService.pkcs7Signature(any(), anyBoolean())).thenReturn(Mono.just(pnSignDocumentResponse));
    }

    void verifyArubaProviderSignCalls(String contentType) {
        switch (contentType) {
            case "application/pdf" -> verify(arubaSignProviderService, times(1)).signPdfDocument(any(), anyBoolean());
            case "application/xml" -> verify(arubaSignProviderService, times(1)).signXmlDocument(any(), anyBoolean());
            case "other" -> verify(arubaSignProviderService, times(1)).pkcs7Signature(any(), anyBoolean());
        }
    }

    void verifyNamirialProviderSignCalls(String contentType) {
        switch (contentType) {
            case "application/pdf" -> verify(namirialProviderService, times(1)).signPdfDocument(any(), anyBoolean());
            case "application/xml" -> verify(namirialProviderService, times(1)).signXmlDocument(any(), anyBoolean());
            case "other" -> verify(namirialProviderService, times(1)).pkcs7Signature(any(), anyBoolean());
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

    private S3EventNotificationRecord createS3Event(String eventName, String fileKey) {
        S3EventNotificationRecord event = new S3EventNotificationRecord();
        S3Bucket s3Bucket = new S3Bucket("", new UserIdentity(""), "");
        S3Object s3Object = new S3Object(fileKey, 10L, "", "", "");
        S3 s3 = new S3("", s3Bucket, s3Object, "");
        event.setS3(s3);
        event.setEventName(eventName);
        return event;
    }

    private S3EventNotificationRecord createS3Event(String eventName) {
        return createS3Event(eventName, FILE_KEY);
    }
*/

}