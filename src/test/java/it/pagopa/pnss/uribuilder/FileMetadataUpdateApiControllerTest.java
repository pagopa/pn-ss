package it.pagopa.pnss.uribuilder;

import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.*;
import it.pagopa.pnss.common.DocTypesConstant;
import it.pagopa.pnss.common.client.DocTypesClientCall;
import it.pagopa.pnss.common.client.DocumentClientCall;
import it.pagopa.pnss.common.client.ScadenzaDocumentiClientCall;
import it.pagopa.pnss.common.client.UserConfigurationClientCall;
import it.pagopa.pnss.common.client.exception.DocumentKeyNotPresentException;
import it.pagopa.pnss.common.constant.Constant;
import it.pagopa.pnss.common.exception.PatchDocumentException;
import it.pagopa.pnss.configurationproperties.BucketName;
import it.pagopa.pnss.testutils.annotation.SpringBootTestWebEnv;
import it.pagopa.pnss.transformation.service.S3Service;
import it.pagopa.pnss.utils.IgnoredUpdateMetadataConfigTestSetup;
import lombok.CustomLog;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.Tag;

import java.sql.Date;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

import static it.pagopa.pnss.common.constant.Constant.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@SpringBootTestWebEnv
@AutoConfigureWebTestClient(timeout = "36000")
@CustomLog
class FileMetadataUpdateApiControllerTest extends IgnoredUpdateMetadataConfigTestSetup {

	@Autowired
	private WebTestClient webClient;

	@Autowired
	private S3Client s3TestClient;

	@Autowired
	private BucketName bucketName;

	@MockitoBean
	private UserConfigurationClientCall userConfigurationClientCall;

	@MockitoSpyBean
	private S3Service s3Service;

	@MockitoSpyBean
	private ScadenzaDocumentiClientCall scadenzaDocumentiClientCall;

	@MockitoBean
	private DocumentClientCall documentClientCall;

	@MockitoBean
	private DocTypesClientCall docTypesClientCall;

	@Value("${header.x-api-key:#{null}}")
	private String xApiKey;

	@Value("${header.x-pagopa-safestorage-cx-id:#{null}}")
	private String X_PAGOPA_SAFESTORAGE_CX_ID;

	@Value("${file.updateMetadata.api.url}")
	private String urlPath;

	private static final String X_PAGO_PA_SAFESTORAGE_CX_ID_VALUE = "CLIENT_ID_123";
	private static final String xPagoPaSafestorageCxIdValue = "CLIENT_ID_123";
	private static final String X_API_KEY_VALUE = "apiKey_value";
	private static final String PN_NOTIFIED_DOCUMENTS = "PN_NOTIFIED_DOCUMENTS";
	private static final String EMPTIED_VALUE= "null";
	private static final Tag FREEZE_TAG = Tag.builder().key("storage_freeze").value(PN_NOTIFIED_DOCUMENTS).build();
	private static final Tag EXPIRY_TAG = Tag.builder().key("storage_expiry").value(PN_NOTIFIED_DOCUMENTS).build();
	private static final ZoneId REFERENCE_TIME_ZONE = ZoneId.of("Europe/Rome");
	private static final DateTimeFormatter UTC_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX").withZone(ZoneOffset.UTC);

	private WebTestClient.ResponseSpec fileMetadataUpdateTestCall(UpdateFileMetadataRequest updateFileMetadataRequest, String documentKey) {

		webClient.mutate().responseTimeout(Duration.ofMillis(30000)).build();

		return webClient.post()
				.uri(uriBuilder -> uriBuilder.path(urlPath).queryParam("metadataOnly", false).build(documentKey))
				.header(X_PAGOPA_SAFESTORAGE_CX_ID, xPagoPaSafestorageCxIdValue)
				.header(xApiKey, X_API_KEY_VALUE)
				.header(HttpHeaders.ACCEPT, APPLICATION_JSON_VALUE)
				.bodyValue(updateFileMetadataRequest)
				.exchange();
	}

	@BeforeEach
	public void createUserConfiguration() {
		var userConfiguration =
				new UserConfiguration().name(X_PAGO_PA_SAFESTORAGE_CX_ID_VALUE).apiKey(X_API_KEY_VALUE).canModifyStatus(List.of(
						DocTypesConstant.PN_NOTIFICATION_ATTACHMENTS,
						DocTypesConstant.PN_AAR));
		var userConfigurationResponse = new UserConfigurationResponse().userConfiguration(userConfiguration);
		when(userConfigurationClientCall.getUser(anyString())).thenReturn(Mono.just(userConfigurationResponse));
	}

	@Test
	void testDocumentKeyNotPresent() {
		when(documentClientCall.getDocument(anyString())).thenReturn(Mono.error(new DocumentKeyNotPresentException("keyFile")));
		fileMetadataUpdateTestCall(new UpdateFileMetadataRequest(), X_PAGOPA_SAFESTORAGE_CX_ID).expectStatus().isNotFound();
	}

	@Test
	void testErrorStatus() {
		var documentType1 = new DocumentType().statuses(Map.ofEntries(Map.entry(PRELOADED, new CurrentStatus()))).tipoDocumento(
				DocTypesConstant.PN_AAR);
		var document = new DocumentResponseDocument().documentType(documentType1).documentState(BOOKED);
        var documentResponse = new DocumentResponse().document(document);
		when(documentClientCall.getDocument(anyString())).thenReturn(Mono.just(documentResponse));

		var documentType2 = new DocumentType().statuses(Map.ofEntries(Map.entry(PRELOADED, new CurrentStatus().technicalState(""))))
				.tipoDocumento(DocTypesConstant.PN_AAR);
		var documentTypeResponse = new DocumentTypeResponse().docType(documentType2);
		when(docTypesClientCall.getdocTypes(anyString())).thenReturn(Mono.just(documentTypeResponse));

		fileMetadataUpdateTestCall(new UpdateFileMetadataRequest().status(PRELOADED), X_PAGOPA_SAFESTORAGE_CX_ID).expectStatus()
				.isBadRequest();
	}

	@Test
	void testErrorTechnicalStatus() {
		var documentType1 = new DocumentType().statuses(Map.ofEntries(Map.entry(PRELOADED, new CurrentStatus()))).tipoDocumento(
				DocTypesConstant.PN_AAR);
		var document = new DocumentResponseDocument().documentType(documentType1).documentState(BOOKED);
        var documentResponse = new DocumentResponse().document(document);
		when(documentClientCall.getDocument(anyString())).thenReturn(Mono.just(documentResponse));

		var documentType2 = new DocumentType().statuses(Map.ofEntries(Map.entry(ATTACHED, new CurrentStatus().technicalState(""))))
				.tipoDocumento(DocTypesConstant.PN_AAR);
		var documentTypeResponse = new DocumentTypeResponse().docType(documentType2);
		when(docTypesClientCall.getdocTypes(anyString())).thenReturn(Mono.just(documentTypeResponse));

		fileMetadataUpdateTestCall(new UpdateFileMetadataRequest().status(ATTACHED), X_PAGOPA_SAFESTORAGE_CX_ID).expectStatus()
				.isBadRequest();
	}

	@Test
	void testErrorUserConfigurationWithoutPrivilege() {
		var userWhoCannotEdit = new UserConfigurationResponse().userConfiguration(new UserConfiguration().canModifyStatus(null)
				.name(X_PAGO_PA_SAFESTORAGE_CX_ID_VALUE)
				.apiKey(X_API_KEY_VALUE));
		when(userConfigurationClientCall.getUser(anyString())).thenReturn(Mono.just(userWhoCannotEdit));

		var documentType1 = new DocumentType().statuses(Map.ofEntries(Map.entry(SAVED, new CurrentStatus()))).tipoDocumento(DocTypesConstant.PN_AAR);
		var document = new DocumentResponseDocument().documentType(documentType1).documentState(BOOKED);
        var documentResponse = new DocumentResponse().document(document);
		when(documentClientCall.getDocument(anyString())).thenReturn(Mono.just(documentResponse));

		var documentType2 = new DocumentType().statuses(Map.ofEntries(Map.entry(SAVED,
						new CurrentStatus().technicalState(Constant.AVAILABLE))))
				.tipoDocumento(DocTypesConstant.PN_AAR);
		var documentTypeResponse = new DocumentTypeResponse().docType(documentType2);
		when(docTypesClientCall.getdocTypes(anyString())).thenReturn(Mono.just(documentTypeResponse));

		fileMetadataUpdateTestCall(new UpdateFileMetadataRequest().status(SAVED), X_PAGOPA_SAFESTORAGE_CX_ID).expectStatus().isForbidden();
	}


	@Test
	void testErrorLookUpStatus() {
		var documentType1 = new DocumentType().statuses(Map.ofEntries(Map.entry(PRELOADED, new CurrentStatus()))).tipoDocumento(
				DocTypesConstant.PN_AAR);
		var document = new DocumentResponseDocument().documentType(documentType1).documentState(BOOKED);
        var documentResponse = new DocumentResponse().document(document);
		when(documentClientCall.getDocument(anyString())).thenReturn(Mono.just(documentResponse));

		var documentType2 = new DocumentType().statuses(Map.ofEntries(Map.entry(ATTACHED, new CurrentStatus().technicalState("")))).tipoDocumento(
				DocTypesConstant.PN_AAR);
		var documentTypeResponse = new DocumentTypeResponse().docType(documentType2);
		when(docTypesClientCall.getdocTypes(anyString())).thenReturn(Mono.just(documentTypeResponse));

		fileMetadataUpdateTestCall(new UpdateFileMetadataRequest().status(PRELOADED), X_PAGOPA_SAFESTORAGE_CX_ID).expectStatus().isBadRequest();
	}

	@Test
	void testFileMetadataUpdateOk() {
		Map<String, CurrentStatus> statuses = Map.ofEntries(Map.entry(SAVED, new CurrentStatus().technicalState(AVAILABLE).storage("storageType")));
		var documentType1 = new DocumentType().statuses(statuses).tipoDocumento(DocTypesConstant.PN_AAR);
		var document = new DocumentResponseDocument().documentType(documentType1).documentState(BOOKED);
        var documentResponse = new DocumentResponse().document(document);
		when(documentClientCall.getDocument(anyString())).thenReturn(Mono.just(documentResponse));
		when(documentClientCall.patchDocument(anyString(), anyString(), anyString(), any())).thenReturn(Mono.just(documentResponse));
		fileMetadataUpdateTestCall(new UpdateFileMetadataRequest().status(SAVED), X_PAGOPA_SAFESTORAGE_CX_ID).expectStatus().isOk();
	}

	@Test
	void testIgnoreS3UpdateMetadataOk() {
		//The fileKey is in ignored-update-metadata.csv file
		String fileKey = "fileKeyToIgnoreUpdateMetadata1";
		addFileToBucket(fileKey, bucketName.ssHotName());

		Map<String, CurrentStatus> statuses = Map.ofEntries(Map.entry(SAVED, new CurrentStatus().technicalState(AVAILABLE).storage("storageType")));
		var documentType1 = new DocumentType().statuses(statuses).tipoDocumento(DocTypesConstant.PN_AAR);
		var document = new DocumentResponseDocument().documentState(AVAILABLE).documentType(documentType1);
		var documentResponse = new DocumentResponse().document(document);
		when(documentClientCall.getDocument(anyString())).thenReturn(Mono.just(documentResponse));
		when(documentClientCall.patchDocument(anyString(), anyString(), anyString(), any())).thenReturn(Mono.just(documentResponse));
		doReturn(Mono.just(new ScadenzaDocumentiResponse())).when(scadenzaDocumentiClientCall).insertOrUpdateScadenzaDocumenti(any(ScadenzaDocumentiInput.class));

		var documentType2 = new DocumentType().statuses(statuses).tipoDocumento(DocTypesConstant.PN_AAR);
		var documentTypeResponse = new DocumentTypeResponse().docType(documentType2);
		when(docTypesClientCall.getdocTypes(anyString())).thenReturn(Mono.just(documentTypeResponse));

		fileMetadataUpdateTestCall(new UpdateFileMetadataRequest().retentionUntil(Date.from(Instant.now())), fileKey).expectStatus().isOk();
		verify(s3Service, never()).putObjectTagging(anyString(), anyString(), any());

		//Clean-up
		s3TestClient.deleteObject(builder -> builder.bucket(bucketName.ssHotName()).key(fileKey));
	}

	private void addFileToBucket(String fileName, String bucketName) {
		byte[] fileBytes = new byte[10];
		PutObjectRequest request = PutObjectRequest.builder()
				.bucket(bucketName)
				.key(fileName)
				.contentMD5(new String(Base64.encodeBase64(DigestUtils.md5(fileBytes)))).build();
		s3TestClient.putObject(request, RequestBody.fromBytes(fileBytes));
	}


	@Test
	void testFileMetadataUpdateStatusDeleted() {
		Map<String, CurrentStatus> statuses = Map.ofEntries(Map.entry(SAVED, new CurrentStatus().technicalState(AVAILABLE).storage("storageType")));
		var documentType1 = new DocumentType().statuses(statuses).tipoDocumento(DocTypesConstant.PN_AAR);
		var document = new DocumentResponseDocument().documentType(documentType1).documentState(DELETED);
		var documentResponse = new DocumentResponse().document(document);
		when(documentClientCall.getDocument(anyString())).thenReturn(Mono.just(documentResponse));
		when(documentClientCall.patchDocument(anyString(), anyString(), anyString(), any())).thenReturn(Mono.error(new PatchDocumentException("Document deleted", HttpStatus.GONE)));

		var documentType2 = new DocumentType().statuses(Map.ofEntries(Map.entry(SAVED,
						new CurrentStatus().technicalState(Constant.TECHNICAL_STATUS_AVAILABLE))))
				.tipoDocumento(DocTypesConstant.PN_AAR);
		var documentTypeResponse = new DocumentTypeResponse().docType(documentType2);
		when(docTypesClientCall.getdocTypes(anyString())).thenReturn(Mono.just(documentTypeResponse));

		fileMetadataUpdateTestCall(new UpdateFileMetadataRequest().status(SAVED), X_PAGOPA_SAFESTORAGE_CX_ID).expectStatus().isEqualTo(410);
	}

	private String formatUtc(Instant instant) {
		return UTC_FORMATTER.format(instant);
	}

	private String formatEndOfDay(Instant instant) {
		return UTC_FORMATTER.format(instant.atZone(REFERENCE_TIME_ZONE).toLocalDate().atTime(23, 59, 59).atZone(REFERENCE_TIME_ZONE).toInstant());
	}

	private DocumentResponse mockDocument(String storedRetentionUntil) {
		Map<String, CurrentStatus> statuses = Map.ofEntries(Map.entry(SAVED, new CurrentStatus().technicalState(AVAILABLE).storage("storageType")));
		var documentType = new DocumentType().statuses(statuses).tipoDocumento(DocTypesConstant.PN_AAR);
		var document = new DocumentResponseDocument().documentType(documentType).documentState(AVAILABLE).retentionUntil(storedRetentionUntil);
		var documentResponse = new DocumentResponse().document(document);
		when(documentClientCall.getDocument(anyString())).thenReturn(Mono.just(documentResponse));
		when(documentClientCall.patchDocument(anyString(), anyString(), anyString(), any())).thenReturn(Mono.just(documentResponse));
		return documentResponse;
	}

	private DocumentChanges capturePatchedChanges() {
		ArgumentCaptor<DocumentChanges> captor = ArgumentCaptor.forClass(DocumentChanges.class);
		verify(documentClientCall).patchDocument(anyString(), anyString(), anyString(), captor.capture());
		return captor.getValue();
	}

	@Test
	void testAvailableUntilAlreadyExpired() {
		mockDocument(null);

		var availableUntil = Date.from(Instant.now().minus(Duration.ofDays(1)));
		fileMetadataUpdateTestCall(new UpdateFileMetadataRequest().availableUntil(availableUntil), X_PAGOPA_SAFESTORAGE_CX_ID).expectStatus()
				.isBadRequest();

		verify(documentClientCall, never()).patchDocument(anyString(), anyString(), anyString(), any());
	}

	@Test
	void testAvailableUntilCurrentDayNormalizedToEndOfDay() {
		mockDocument(formatUtc(Instant.now().plus(Duration.ofDays(30)).truncatedTo(ChronoUnit.SECONDS)));

		var now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
		fileMetadataUpdateTestCall(new UpdateFileMetadataRequest().availableUntil(Date.from(now)), X_PAGOPA_SAFESTORAGE_CX_ID).expectStatus()
				.isOk();

		var documentChanges = capturePatchedChanges();
		Assertions.assertEquals(formatEndOfDay(now), documentChanges.getAvailableUntil());
		Assertions.assertNull(documentChanges.getRetentionUntil());
		verify(scadenzaDocumentiClientCall, never()).insertOrUpdateScadenzaDocumenti(any(ScadenzaDocumentiInput.class));
	}

	@Test
	void testAvailableUntilWithoutRetentionUntilSetsRetention() {
		String fileKey = "fileKeyAvailableUntilWithoutRetention";
		addFileToBucket(fileKey, bucketName.ssHotName());

		mockDocument(null);
		doReturn(Mono.just(new ScadenzaDocumentiResponse())).when(scadenzaDocumentiClientCall).insertOrUpdateScadenzaDocumenti(any(ScadenzaDocumentiInput.class));

		var availableUntil = Instant.now().plus(Duration.ofDays(20)).truncatedTo(ChronoUnit.SECONDS);
		fileMetadataUpdateTestCall(new UpdateFileMetadataRequest().availableUntil(Date.from(availableUntil)), fileKey).expectStatus().isOk();

		var documentChanges = capturePatchedChanges();
		Assertions.assertEquals(formatEndOfDay(availableUntil), documentChanges.getAvailableUntil());
		Assertions.assertEquals(formatEndOfDay(availableUntil), documentChanges.getRetentionUntil());
		verify(scadenzaDocumentiClientCall).insertOrUpdateScadenzaDocumenti(any(ScadenzaDocumentiInput.class));

		s3TestClient.deleteObject(builder -> builder.bucket(bucketName.ssHotName()).key(fileKey));
	}

	@Test
	void testAvailableUntilBeforeRetentionUntilLeavesRetentionUnchanged() {
		var storedRetentionUntil = Instant.now().plus(Duration.ofDays(30)).truncatedTo(ChronoUnit.SECONDS);
		mockDocument(formatUtc(storedRetentionUntil));

		var availableUntil = Instant.now().plus(Duration.ofDays(10)).truncatedTo(ChronoUnit.SECONDS);
		fileMetadataUpdateTestCall(new UpdateFileMetadataRequest().availableUntil(Date.from(availableUntil)), X_PAGOPA_SAFESTORAGE_CX_ID)
				.expectStatus().isOk();

		var documentChanges = capturePatchedChanges();
		Assertions.assertEquals(formatEndOfDay(availableUntil), documentChanges.getAvailableUntil());
		Assertions.assertNull(documentChanges.getRetentionUntil());
		verify(scadenzaDocumentiClientCall, never()).insertOrUpdateScadenzaDocumenti(any(ScadenzaDocumentiInput.class));
	}

	@Test
	void testAvailableUntilAfterRetentionUntilExtendsRetention() {
		String fileKey = "fileKeyAvailableUntilExtendsRetention";
		addFileToBucket(fileKey, bucketName.ssHotName());

		var storedRetentionUntil = Instant.now().plus(Duration.ofDays(10)).truncatedTo(ChronoUnit.SECONDS);
		mockDocument(formatUtc(storedRetentionUntil));
		doReturn(Mono.just(new ScadenzaDocumentiResponse())).when(scadenzaDocumentiClientCall).insertOrUpdateScadenzaDocumenti(any(ScadenzaDocumentiInput.class));

		var availableUntil = Instant.now().plus(Duration.ofDays(20)).truncatedTo(ChronoUnit.SECONDS);
		fileMetadataUpdateTestCall(new UpdateFileMetadataRequest().availableUntil(Date.from(availableUntil)), fileKey).expectStatus().isOk();

		var documentChanges = capturePatchedChanges();
		Assertions.assertEquals(formatEndOfDay(availableUntil), documentChanges.getAvailableUntil());
		Assertions.assertEquals(formatEndOfDay(availableUntil), documentChanges.getRetentionUntil());

		ArgumentCaptor<ScadenzaDocumentiInput> captor = ArgumentCaptor.forClass(ScadenzaDocumentiInput.class);
		verify(scadenzaDocumentiClientCall).insertOrUpdateScadenzaDocumenti(captor.capture());
		Assertions.assertEquals(Instant.from(UTC_FORMATTER.parse(formatEndOfDay(availableUntil))).getEpochSecond(), captor.getValue().getRetentionUntil());
		verify(s3Service).putObjectTagging(anyString(), anyString(), any());

		s3TestClient.deleteObject(builder -> builder.bucket(bucketName.ssHotName()).key(fileKey));
	}

	@Test
	void testAvailableUntilEvaluatedAgainstRetentionUntilOfSameRequest() {
		String fileKey = "fileKeyAvailableUntilWithRetentionUntil";
		addFileToBucket(fileKey, bucketName.ssHotName());

		var storedRetentionUntil = Instant.now().plus(Duration.ofDays(30)).truncatedTo(ChronoUnit.SECONDS);
		mockDocument(formatUtc(storedRetentionUntil));
		doReturn(Mono.just(new ScadenzaDocumentiResponse())).when(scadenzaDocumentiClientCall).insertOrUpdateScadenzaDocumenti(any(ScadenzaDocumentiInput.class));

		var requestedRetentionUntil = Instant.now().plus(Duration.ofDays(40)).truncatedTo(ChronoUnit.SECONDS);
		var availableUntil = Instant.now().plus(Duration.ofDays(35)).truncatedTo(ChronoUnit.SECONDS);
		fileMetadataUpdateTestCall(new UpdateFileMetadataRequest().retentionUntil(Date.from(requestedRetentionUntil))
				.availableUntil(Date.from(availableUntil)), fileKey).expectStatus().isOk();

		var documentChanges = capturePatchedChanges();
		Assertions.assertEquals(formatEndOfDay(availableUntil), documentChanges.getAvailableUntil());
		Assertions.assertEquals(formatUtc(requestedRetentionUntil), documentChanges.getRetentionUntil());

		s3TestClient.deleteObject(builder -> builder.bucket(bucketName.ssHotName()).key(fileKey));
	}
}
