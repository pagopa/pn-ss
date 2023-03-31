package it.pagopa.pnss.uribuilder;

import it.pagopa.pn.template.internal.rest.v1.dto.*;
import it.pagopa.pn.template.rest.v1.dto.UpdateFileMetadataRequest;
import it.pagopa.pnss.common.Constant;
import it.pagopa.pnss.common.client.DocTypesClientCall;
import it.pagopa.pnss.common.client.DocumentClientCall;
import it.pagopa.pnss.common.client.UserConfigurationClientCall;
import it.pagopa.pnss.common.client.exception.DocumentKeyNotPresentException;
import it.pagopa.pnss.common.client.exception.DocumentTypeNotPresentException;
import it.pagopa.pnss.testutils.annotation.SpringBootTestWebEnv;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static it.pagopa.pnss.common.Constant.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@SpringBootTestWebEnv
@AutoConfigureWebTestClient
@Slf4j
class FileMetadataUpdateApiControllerTest {

	@Autowired
	private WebTestClient webClient;

	@MockBean
	private UserConfigurationClientCall userConfigurationClientCall;

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
				new UserConfiguration().name(X_PAGO_PA_SAFESTORAGE_CX_ID_VALUE).apiKey(X_API_KEY_VALUE).canModifyStatus(List.of(PN_AAR));
		var userConfigurationResponse = new UserConfigurationResponse().userConfiguration(userConfiguration);
		when(userConfigurationClientCall.getUser(anyString())).thenReturn(Mono.just(userConfigurationResponse));
	}

	@Test
	void testDocumentKeyNotPresent() {
		when(documentClientCall.getdocument(anyString())).thenReturn(Mono.error(new DocumentKeyNotPresentException("keyFile")));
		fileMetadataUpdateTestCall(new UpdateFileMetadataRequest(), X_PAGOPA_SAFESTORAGE_CX_ID).expectStatus().isNotFound();
	}

	@Test
	void testErrorStatus() {
		var documentType1 = new DocumentType().statuses(Map.ofEntries(Map.entry(PRELOADED, new CurrentStatus()))).tipoDocumento(PN_AAR);
		var document = new Document().documentType(documentType1);
		var documentResponse = new DocumentResponse().document(document);
		when(documentClientCall.getdocument(anyString())).thenReturn(Mono.just(documentResponse));

		var documentType2 = new DocumentType().statuses(Map.ofEntries(Map.entry(PRELOADED, new CurrentStatus().technicalState(""))))
											  .tipoDocumento(PN_AAR);
		var documentTypeResponse = new DocumentTypeResponse().docType(documentType2);
		when(docTypesClientCall.getdocTypes(anyString())).thenReturn(Mono.just(documentTypeResponse));

		fileMetadataUpdateTestCall(new UpdateFileMetadataRequest().status(PRELOADED), X_PAGOPA_SAFESTORAGE_CX_ID).expectStatus()
																												 .isBadRequest();
	}

	@Test
	void testErrorTechnicalStatus() {
		var documentType1 = new DocumentType().statuses(Map.ofEntries(Map.entry(PRELOADED, new CurrentStatus()))).tipoDocumento(PN_AAR);
		var document = new Document().documentType(documentType1);
		var documentResponse = new DocumentResponse().document(document);
		when(documentClientCall.getdocument(anyString())).thenReturn(Mono.just(documentResponse));

		var documentType2 = new DocumentType().statuses(Map.ofEntries(Map.entry(ATTACHED, new CurrentStatus().technicalState(""))))
											  .tipoDocumento(PN_AAR);
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

		var documentType1 = new DocumentType().statuses(Map.ofEntries(Map.entry(SAVED, new CurrentStatus()))).tipoDocumento(PN_AAR);
		var document = new Document().documentType(documentType1);
		var documentResponse = new DocumentResponse().document(document);
		when(documentClientCall.getdocument(anyString())).thenReturn(Mono.just(documentResponse));

		var documentType2 = new DocumentType().statuses(Map.ofEntries(Map.entry(SAVED,
																				new CurrentStatus().technicalState(Constant.TECHNICAL_STATUS_AVAILABLE))))
											  .tipoDocumento(PN_AAR);
		var documentTypeResponse = new DocumentTypeResponse().docType(documentType2);
		when(docTypesClientCall.getdocTypes(anyString())).thenReturn(Mono.just(documentTypeResponse));

		fileMetadataUpdateTestCall(new UpdateFileMetadataRequest().status(SAVED), X_PAGOPA_SAFESTORAGE_CX_ID).expectStatus().isForbidden();
	}

    @Test
    void testErrorLookUpDocTypes() {
        var documentType1 = new DocumentType().statuses(Map.ofEntries(Map.entry(PRELOADED, new CurrentStatus()))).tipoDocumento(PN_AAR);
        var document = new Document().documentType(documentType1);
        var documentResponse = new DocumentResponse().document(document);
        when(documentClientCall.getdocument(anyString())).thenReturn(Mono.just(documentResponse));

        when(docTypesClientCall.getdocTypes(anyString())).thenReturn(Mono.error(new DocumentKeyNotPresentException("keyFile")));

        fileMetadataUpdateTestCall(new UpdateFileMetadataRequest().status(PRELOADED), X_PAGOPA_SAFESTORAGE_CX_ID).expectStatus().isNotFound();
    }

    @Test
    void testErrorLookUpStatus() {
        var documentType1 = new DocumentType().statuses(Map.ofEntries(Map.entry(PRELOADED, new CurrentStatus()))).tipoDocumento(PN_AAR);
        var document = new Document().documentType(documentType1);
        var documentResponse = new DocumentResponse().document(document);
        when(documentClientCall.getdocument(anyString())).thenReturn(Mono.just(documentResponse));

        var documentType2 = new DocumentType().statuses(Map.ofEntries(Map.entry(ATTACHED, new CurrentStatus().technicalState("")))).tipoDocumento(PN_AAR);
        var documentTypeResponse = new DocumentTypeResponse().docType(documentType2);
        when(docTypesClientCall.getdocTypes(anyString())).thenReturn(Mono.just(documentTypeResponse));

        fileMetadataUpdateTestCall(new UpdateFileMetadataRequest().status(PRELOADED), X_PAGOPA_SAFESTORAGE_CX_ID).expectStatus().isBadRequest();
    }

	@Test
	void testFileMetadataUpdateOk() {
		var documentType1 = new DocumentType().statuses(Map.ofEntries(Map.entry(SAVED, new CurrentStatus()))).tipoDocumento(PN_AAR);
		var document = new Document().documentType(documentType1);
		var documentResponse = new DocumentResponse().document(document);
		when(documentClientCall.getdocument(anyString())).thenReturn(Mono.just(documentResponse));
		when(documentClientCall.patchdocument(anyString(), anyString(), anyString(), any())).thenReturn(Mono.just(documentResponse));

		var documentType2 = new DocumentType().statuses(Map.ofEntries(Map.entry(SAVED,
																				new CurrentStatus().technicalState(Constant.TECHNICAL_STATUS_AVAILABLE))))
											  .tipoDocumento(PN_AAR);
		var documentTypeResponse = new DocumentTypeResponse().docType(documentType2);
		when(docTypesClientCall.getdocTypes(anyString())).thenReturn(Mono.just(documentTypeResponse));

		fileMetadataUpdateTestCall(new UpdateFileMetadataRequest().status(SAVED), X_PAGOPA_SAFESTORAGE_CX_ID).expectStatus().isOk();
	}
}
