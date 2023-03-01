package it.pagopa.pnss.uribuilder;

import static it.pagopa.pnss.common.Constant.ATTACHED;
import static it.pagopa.pnss.common.Constant.PN_AAR;
import static it.pagopa.pnss.common.Constant.PRELOADED;
import static it.pagopa.pnss.common.Constant.SAVED;

import java.time.Duration;
import java.util.Arrays;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ReactiveHttpOutputMessage;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserter;
import org.springframework.web.reactive.function.BodyInserters;

import it.pagopa.pn.template.internal.rest.v1.dto.CurrentStatus;
import it.pagopa.pn.template.internal.rest.v1.dto.Document;
import it.pagopa.pn.template.internal.rest.v1.dto.DocumentResponse;
import it.pagopa.pn.template.internal.rest.v1.dto.DocumentType;
import it.pagopa.pn.template.internal.rest.v1.dto.UserConfiguration;
import it.pagopa.pn.template.internal.rest.v1.dto.UserConfigurationResponse;
import it.pagopa.pn.template.rest.v1.dto.UpdateFileMetadataRequest;
import it.pagopa.pnss.common.client.DocumentClientCall;
import it.pagopa.pnss.common.client.UserConfigurationClientCall;
import it.pagopa.pnss.common.client.exception.DocumentKeyNotPresentException;
import it.pagopa.pnss.testutils.annotation.SpringBootTestWebEnv;
import it.pagopa.pnss.uribuilder.service.UriBuilderService;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@SpringBootTestWebEnv
@AutoConfigureWebTestClient
@Slf4j
public class FileMetadataUpdateTest {

	@Value("${header.x-api-key:#{null}}")
	private String xApiKey;
	@Value("${header.x-pagopa-safestorage-cx-id:#{null}}")
	private String X_PAGOPA_SAFESTORAGE_CX_ID;
//    public static final String X_PAGOPA_SAFESTORAGE_CX_ID = "x-pagopa-safestorage-cx-id";
	private static final String X_PAGO_PA_SAFESTORAGE_CX_ID_VALUE = "CLIENT_ID_123";
	private static final String xApiKeyValue = "apiKey_value";
	private static final String xPagoPaSafestorageCxIdValue = "CLIENT_ID_123";
	private static final DocumentResponse DOCUMENT_RESPONSE = new DocumentResponse()
			.document(new Document().documentKey("documentKey"));
	private static final String X_API_KEY_VALUE = "apiKey_value";
	@Value("${file.updateMetadata.api.url}")
	private String urlPath;

	@Autowired
	private WebTestClient webClient;

	@MockBean
	UserConfigurationClientCall userConfigurationClientCall;
	@Autowired
	private UriBuilderService uriBuilderService;

	@MockBean
	DocumentClientCall documentClientCall;

	private WebTestClient.ResponseSpec fileMetadataUpdateTestCall(
			BodyInserter<UpdateFileMetadataRequest, ReactiveHttpOutputMessage> bodyInserter, String docuemntKey) {
		this.webClient.mutate().responseTimeout(Duration.ofMillis(30000)).build();

		return this.webClient.post().uri(uriBuilder -> uriBuilder.path(urlPath)
				// ... building a URI
				.build(docuemntKey)).header(X_PAGOPA_SAFESTORAGE_CX_ID, xPagoPaSafestorageCxIdValue)
				.header(xApiKey, X_API_KEY_VALUE).header(HttpHeaders.ACCEPT, "application/json").body(bodyInserter)
				.attribute("metadataOnly", false).exchange();
	}

	@BeforeEach
	private void createUserConfiguration() {

		log.info("createUserConfiguration() : START");

		UserConfiguration userConfiguration = new UserConfiguration();
		userConfiguration.setName(X_PAGO_PA_SAFESTORAGE_CX_ID_VALUE);
		userConfiguration.setApiKey(X_API_KEY_VALUE);

		UserConfigurationResponse userConfig = new UserConfigurationResponse();
		userConfig.setUserConfiguration(userConfiguration);

		Mono<UserConfigurationResponse> userConfigurationResponse = Mono.just(userConfig);
		Mockito.doReturn(userConfigurationResponse).when(userConfigurationClientCall).getUser(Mockito.any());

		log.info("createUserConfiguration() : END");
	}

	@Test
	void testDocumentKeyNotPresent() throws Exception {
		UpdateFileMetadataRequest req = new UpdateFileMetadataRequest();
		Mockito.when(documentClientCall.getdocument(Mockito.any()))
				.thenReturn(Mono.error(new DocumentKeyNotPresentException("keyFile")));

		WebTestClient.ResponseSpec responseSpec = fileMetadataUpdateTestCall(BodyInserters.fromValue(req),
				X_PAGOPA_SAFESTORAGE_CX_ID).expectStatus().isNotFound();

	}

	@Test
	void testErrorStatus() throws Exception {
		UpdateFileMetadataRequest req = new UpdateFileMetadataRequest();
		req.setStatus("PRELOADED");
		DocumentResponse resp = new DocumentResponse();
		Document document = new Document();
		DocumentType documentType = new DocumentType();
		documentType.setStatuses(Map.ofEntries(Map.entry("PRELOADED", new CurrentStatus())));
		document.setDocumentType(documentType);
		resp.setDocument(document);
		Mono<DocumentResponse> monoResp = Mono.just(resp);

		Mockito.doReturn(monoResp).when(documentClientCall).getdocument(Mockito.any());

		fileMetadataUpdateTestCall(BodyInserters.fromValue(req), X_PAGOPA_SAFESTORAGE_CX_ID).expectStatus()
				.isBadRequest();

	}

	@Test
	void testErrorTechnicalStatus() throws Exception {
		UpdateFileMetadataRequest req = new UpdateFileMetadataRequest();
		req.setStatus(ATTACHED);
		DocumentResponse resp = new DocumentResponse();
		Document document = new Document();
		DocumentType documentType = new DocumentType();
		documentType.setStatuses(Map.ofEntries(Map.entry(PRELOADED, new CurrentStatus())));
		documentType.setTipoDocumento(PN_AAR);
		document.setDocumentType(documentType);
		resp.setDocument(document);
		Mono<DocumentResponse> monoResp = Mono.just(resp);

		Mockito.doReturn(monoResp).when(documentClientCall).getdocument(Mockito.any());

		fileMetadataUpdateTestCall(BodyInserters.fromValue(req), X_PAGOPA_SAFESTORAGE_CX_ID).expectStatus()
				.isBadRequest();

	}

	@Test
	void testErrorUserCongigurationWithoutPrivileg() throws Exception {
		UpdateFileMetadataRequest req = new UpdateFileMetadataRequest();
		req.setStatus(SAVED);
		DocumentResponse resp = new DocumentResponse();
		Document document = new Document();
		DocumentType documentType = new DocumentType();
		documentType.setStatuses(Map.ofEntries(Map.entry(SAVED, new CurrentStatus())));
		documentType.setTipoDocumento(PN_AAR);
		document.setDocumentType(documentType);
		resp.setDocument(document);
		Mono<DocumentResponse> monoResp = Mono.just(resp);

		Mockito.doReturn(monoResp).when(documentClientCall).getdocument(Mockito.any());

		WebTestClient.ResponseSpec responseSpec = fileMetadataUpdateTestCall(BodyInserters.fromValue(req),
				X_PAGOPA_SAFESTORAGE_CX_ID).expectStatus().isForbidden();

	}

	@Test
	void testFileMetadataUpgateOk() throws Exception {
		UpdateFileMetadataRequest req = new UpdateFileMetadataRequest();
		req.setStatus(SAVED);
		DocumentResponse resp = new DocumentResponse();
		Document document = new Document();
		DocumentType documentType = new DocumentType();
		documentType.setStatuses(Map.ofEntries(Map.entry(SAVED, new CurrentStatus())));
		documentType.setTipoDocumento(PN_AAR);
		document.setDocumentType(documentType);
		resp.setDocument(document);
		Mono<DocumentResponse> monoResp = Mono.just(resp);

		Mockito.doReturn(monoResp).when(documentClientCall).getdocument(Mockito.any());

		UserConfiguration userConfiguration = new UserConfiguration();
		userConfiguration.setName(X_PAGO_PA_SAFESTORAGE_CX_ID_VALUE);
		userConfiguration.setApiKey(X_API_KEY_VALUE);
		userConfiguration.setCanModifyStatus(Arrays.asList(PN_AAR));
		UserConfigurationResponse userConfig = new UserConfigurationResponse();
		userConfig.setUserConfiguration(userConfiguration);

		Mono<UserConfigurationResponse> userConfigurationResponse = Mono.just(userConfig);
		Mockito.doReturn(userConfigurationResponse).when(userConfigurationClientCall).getUser(Mockito.any());

		Mockito.doReturn(monoResp).when(documentClientCall).patchdocument(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());

		WebTestClient.ResponseSpec responseSpec = fileMetadataUpdateTestCall(BodyInserters.fromValue(req),
				X_PAGOPA_SAFESTORAGE_CX_ID).expectStatus().isOk();

	}

}
