package it.pagopa.pnss.uribuilder;

import com.github.dockerjava.zerodep.shaded.org.apache.commons.codec.binary.Base64;
import com.github.dockerjava.zerodep.shaded.org.apache.commons.codec.digest.DigestUtils;
import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.*;
import it.pagopa.pnss.common.client.ScadenzaDocumentiClientCall;
import it.pagopa.pnss.common.constant.Constant;
import it.pagopa.pnss.common.DocTypesConstant;
import it.pagopa.pnss.common.client.DocTypesClientCall;
import it.pagopa.pnss.common.client.DocumentClientCall;
import it.pagopa.pnss.common.client.UserConfigurationClientCall;
import it.pagopa.pnss.common.client.exception.DocumentKeyNotPresentException;
import it.pagopa.pnss.configurationproperties.BucketName;
import it.pagopa.pnss.repositorymanager.exception.DynamoDbException;
import it.pagopa.pnss.testutils.annotation.SpringBootTestWebEnv;
import it.pagopa.pnss.transformation.service.S3Service;
import lombok.CustomLog;
import org.eclipse.angus.mail.iap.ConnectionException;
import org.joda.time.DateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.reactive.server.WebTestClient;

import reactor.core.publisher.Mono;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static it.pagopa.pnss.common.constant.Constant.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@SpringBootTestWebEnv
@AutoConfigureWebTestClient
@CustomLog
class FileMetadataUpdateApiControllerTest {

	@Autowired
	private WebTestClient webClient;

	@Autowired
	private S3Client s3TestClient;

	@Autowired
	private BucketName bucketName;

	@MockBean
	private UserConfigurationClientCall userConfigurationClientCall;

	@SpyBean
	private S3Service s3Service;

	@SpyBean
	private ScadenzaDocumentiClientCall scadenzaDocumentiClientCall;

	@MockBean
	private DocumentClientCall documentClientCall;

	@MockBean
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
	private static final String EMPTIED_VALUE= "";
	private static final Tag FREEZE_TAG = Tag.builder().key("storage_freeze").value(PN_NOTIFIED_DOCUMENTS).build();
	private static final Tag EXPIRY_TAG = Tag.builder().key("storage_expiry").value(PN_NOTIFIED_DOCUMENTS).build();

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
		var document = new Document().documentType(documentType1);
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
		var document = new Document().documentType(documentType1);
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
		var document = new Document().documentType(documentType1);
		var documentResponse = new DocumentResponse().document(document);
		when(documentClientCall.getDocument(anyString())).thenReturn(Mono.just(documentResponse));

		var documentType2 = new DocumentType().statuses(Map.ofEntries(Map.entry(SAVED,
						new CurrentStatus().technicalState(Constant.TECHNICAL_STATUS_AVAILABLE))))
				.tipoDocumento(DocTypesConstant.PN_AAR);
		var documentTypeResponse = new DocumentTypeResponse().docType(documentType2);
		when(docTypesClientCall.getdocTypes(anyString())).thenReturn(Mono.just(documentTypeResponse));

		fileMetadataUpdateTestCall(new UpdateFileMetadataRequest().status(SAVED), X_PAGOPA_SAFESTORAGE_CX_ID).expectStatus().isForbidden();
	}

	@Test
	void testErrorLookUpDocTypes() {
		var documentType1 = new DocumentType().statuses(Map.ofEntries(Map.entry(PRELOADED, new CurrentStatus()))).tipoDocumento(
				DocTypesConstant.PN_AAR);
		var document = new Document().documentType(documentType1);
		var documentResponse = new DocumentResponse().document(document);
		when(documentClientCall.getDocument(anyString())).thenReturn(Mono.just(documentResponse));

		when(docTypesClientCall.getdocTypes(anyString())).thenReturn(Mono.error(new DocumentKeyNotPresentException("keyFile")));

		fileMetadataUpdateTestCall(new UpdateFileMetadataRequest().status(PRELOADED), X_PAGOPA_SAFESTORAGE_CX_ID).expectStatus().isNotFound();
	}

	@Test
	void testErrorLookUpStatus() {
		var documentType1 = new DocumentType().statuses(Map.ofEntries(Map.entry(PRELOADED, new CurrentStatus()))).tipoDocumento(
				DocTypesConstant.PN_AAR);
		var document = new Document().documentType(documentType1);
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
		var documentType1 = new DocumentType().statuses(Map.ofEntries(Map.entry(SAVED, new CurrentStatus()))).tipoDocumento(DocTypesConstant.PN_AAR);
		var document = new Document().documentType(documentType1);
		var documentResponse = new DocumentResponse().document(document);
		when(documentClientCall.getDocument(anyString())).thenReturn(Mono.just(documentResponse));
		when(documentClientCall.patchDocument(anyString(), anyString(), anyString(), any())).thenReturn(Mono.just(documentResponse));

		var documentType2 = new DocumentType().statuses(Map.ofEntries(Map.entry(SAVED,
						new CurrentStatus().technicalState(Constant.TECHNICAL_STATUS_AVAILABLE))))
				.tipoDocumento(DocTypesConstant.PN_AAR);
		var documentTypeResponse = new DocumentTypeResponse().docType(documentType2);
		when(docTypesClientCall.getdocTypes(anyString())).thenReturn(Mono.just(documentTypeResponse));

		fileMetadataUpdateTestCall(new UpdateFileMetadataRequest().status(SAVED), X_PAGOPA_SAFESTORAGE_CX_ID).expectStatus().isOk();
	}
/*
	@Test
	void testFileMetadataUpdateRetentionWithV1TaggingOk() {
		Instant now = Instant.now();
		Tagging taggingV1 = Tagging.builder().tagSet(Tag.builder().key("storageType").value(PN_NOTIFIED_DOCUMENTS).build()).build();
		Tagging expectedTagging = Tagging.builder().tagSet(Set.of(Tag.builder().key("storageType").value(EMPTIED_VALUE).build()
		,FREEZE_TAG,EXPIRY_TAG)).build();

		setupMetadataTests("v1TaggingObject");

		putObjectInBucket("v1TaggingObject",bucketName.ssHotName(),new byte[9],taggingV1);
		fileMetadataUpdateTestCall(new UpdateFileMetadataRequest().retentionUntil(Date.from(now)),"v1TaggingObject").expectStatus().isOk();
		verify(s3Service,times(1)).getObjectTagging("v1TaggingObject",bucketName.ssHotName());
		verify(s3Service, times(1)).putObjectTagging("v1TaggingObject",bucketName.ssHotName(),expectedTagging);
		verify(scadenzaDocumentiClientCall,times(1)).insertOrUpdateScadenzaDocumenti(new ScadenzaDocumentiInput().documentKey("v1TaggingObject").retentionUntil(now.getEpochSecond()));
	}
	@Test
	void testFileMetadataUpdateRetentionWithV2TaggingOk() {
		Instant now = Instant.now();
		Tagging tagging = Tagging.builder().tagSet(Set.of(FREEZE_TAG,EXPIRY_TAG)).build();
		Tagging expectedTagging = Tagging.builder().tagSet(Set.of(Tag.builder().key("storage_expiry").value(EMPTIED_VALUE).build())).build();

		setupMetadataTests("v2TaggingObject");

		putObjectInBucket("v2TaggingObject",bucketName.ssHotName(),new byte[9],tagging);
		fileMetadataUpdateTestCall(new UpdateFileMetadataRequest().retentionUntil(Date.from(now)), "v2TaggingObject").expectStatus().isOk();
		verify(s3Service,times(1)).getObjectTagging("v2TaggingObject",bucketName.ssHotName());
		verify(s3Service, times(1)).putObjectTagging("v2TaggingObject",bucketName.ssHotName(),expectedTagging);
		verify(scadenzaDocumentiClientCall,times(1)).insertOrUpdateScadenzaDocumenti(new ScadenzaDocumentiInput().documentKey("v2TaggingObject").retentionUntil((now.getEpochSecond())));
	}


	@Test
	void testFileMetadataUpdateDynamoError() {
		Instant now = Instant.now();
		Tagging tagging = Tagging.builder().tagSet(Set.of(FREEZE_TAG,EXPIRY_TAG)).build();
		Tagging expectedTagging = Tagging.builder().tagSet(Set.of(Tag.builder().key("storage_expiry").value(EMPTIED_VALUE).build())).build();

		setupMetadataTests("v2TaggingObject");

		putObjectInBucket("v2TaggingObject",bucketName.ssHotName(),new byte[9],tagging);
		when(scadenzaDocumentiClientCall.insertOrUpdateScadenzaDocumenti(any())).thenReturn(Mono.error(new DynamoDbException()));
		fileMetadataUpdateTestCall(new UpdateFileMetadataRequest().retentionUntil(Date.from(now)), "v2TaggingObject").expectStatus().is5xxServerError();
		verify(s3Service,times(1)).getObjectTagging("v2TaggingObject",bucketName.ssHotName());
		verify(s3Service, times(1)).putObjectTagging("v2TaggingObject",bucketName.ssHotName(),expectedTagging);
		verify(scadenzaDocumentiClientCall,times(1)).insertOrUpdateScadenzaDocumenti(new ScadenzaDocumentiInput().documentKey("v2TaggingObject").retentionUntil(now.getEpochSecond()));
	}

	@Test
	void testFileMetadataUpdateWithNoTaggingOk() {
		Instant now = Instant.now();
		String key = "noTaggingObject";
		String bucketName = this.bucketName.ssHotName();
		byte[] fileBytes = new byte[9];
		s3TestClient.putObject(PutObjectRequest.builder()
						.key(key)
						.bucket(bucketName)
						.contentMD5(new String(Base64.encodeBase64(DigestUtils.md5(fileBytes)))).build(),
				RequestBody.fromBytes(fileBytes));

		setupMetadataTests(key);
		fileMetadataUpdateTestCall(new UpdateFileMetadataRequest().retentionUntil(Date.from(now)), key).expectStatus().isOk();

		verify(s3Service,times(1)).getObjectTagging(key,bucketName);
		verify(s3Service, times(0)).putObjectTagging(key,bucketName,Tagging.builder().build());
		verify(scadenzaDocumentiClientCall,times(0)).insertOrUpdateScadenzaDocumenti(new ScadenzaDocumentiInput().documentKey(key).retentionUntil(now.getEpochSecond()));
	}
	@Test
	void testFileMetadataUpdateWithInvalidRetention() {
		//TODO da completare alla creazione della tabella TTS.
	}

	@Test
	void testFileMetadataUpdateWithNonExistentFile(){
		Instant now = Instant.now();
		setupMetadataTests("key");

		fileMetadataUpdateTestCall(new UpdateFileMetadataRequest().retentionUntil(Date.from(now)), "non-existent").expectStatus().isNotFound();
		verify(s3Service,times(1)).getObjectTagging("v2TaggingObject",bucketName.ssHotName());
	}

	@Test
	void testFileMetadataUpdateRetentionFailedConnectionKo() {
		Instant now = Instant.now();

		Tagging expectedTagging = Tagging.builder().tagSet(Set.of(Tag.builder().key("storageType").value(EMPTIED_VALUE).build())).build();

		setupMetadataTests("v1TaggingObject");

		when(documentClientCall.patchDocument(anyString(), anyString(), anyString(), any())).thenThrow(new ConnectionException("Connection error"));

		fileMetadataUpdateTestCall(new UpdateFileMetadataRequest().retentionUntil(Date.from(now)), "non-existent").expectStatus().is5xxServerError();
		verify(s3Service,times(1)).getObjectTagging("v1TaggingObject",bucketName.ssHotName());
		verify(s3Service, times(1)).putObjectTagging("v1TaggingObject",bucketName.ssHotName(),expectedTagging);
		verify(scadenzaDocumentiClientCall,times(1)).insertOrUpdateScadenzaDocumenti(new ScadenzaDocumentiInput().documentKey("v1TaggingObject").retentionUntil(now.getEpochSecond()));
	}

	private void putObjectInBucket(String key, String bucketName, byte[] fileBytes, Tagging tagging) {
		s3TestClient.putObject(PutObjectRequest.builder()
						.key(key)
						.bucket(bucketName)
						.tagging(tagging)
						.contentMD5(new String(Base64.encodeBase64(DigestUtils.md5(fileBytes)))).build(),
				RequestBody.fromBytes(fileBytes));
	}

	private void setupMetadataTests(String documentKey){
		var documentType = new DocumentType().statuses(Map.ofEntries(Map.entry(ATTACHED, new CurrentStatus(){{
			setTechnicalState(ATTACHED);
			setStorage(PN_NOTIFIED_DOCUMENTS);
		}}))).tipoDocumento(DocTypesConstant.PN_NOTIFICATION_ATTACHMENTS);
		var documentTypeResponse = new DocumentTypeResponse().docType(documentType);
		var document = new Document().documentType(documentType).documentState(ATTACHED).documentKey(documentKey);
		var documentResponse = new DocumentResponse().document(document);

		when(documentClientCall.getDocument(anyString())).thenReturn(Mono.just(documentResponse));
		when(documentClientCall.patchDocument(anyString(), anyString(), anyString(), any())).thenReturn(Mono.just(documentResponse));
		when(docTypesClientCall.getdocTypes(anyString())).thenReturn(Mono.just(documentTypeResponse));
	}
	*/
}
