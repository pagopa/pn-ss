package it.pagopa.pnss.indexing;

import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.*;
import it.pagopa.pnss.common.client.DocumentClientCall;
import it.pagopa.pnss.common.client.TagsClientCall;
import it.pagopa.pnss.common.client.UserConfigurationClientCall;
import it.pagopa.pnss.common.client.exception.DocumentKeyNotPresentException;
import it.pagopa.pnss.indexing.service.AdditionalFileTagsService;
import it.pagopa.pnss.testutils.annotation.SpringBootTestWebEnv;
import lombok.CustomLog;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.shaded.org.hamcrest.core.AllOf;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@SpringBootTestWebEnv
@AutoConfigureWebTestClient(timeout = "36000")
@CustomLog
class AdditionalFileTagsUpdateTest {

    @Value("${header.x-api-key:#{null}}")
    private String xApiKey;

    @Value("${header.x-pagopa-safestorage-cx-id:#{null}}")
    private String X_PAGOPA_SAFESTORAGE_CX_ID;
    @Autowired
    private WebTestClient webTestClient;
    @SpyBean
    private AdditionalFileTagsService additionalFileTagsService;
    private static final String PATH_WITH_PARAM = "/safe-storage/v1/files/{fileKey}/tags";
    private static final String PATH_NO_PARAM = "/safe-storage/v1/files/tags";
    @MockBean
    private UserConfigurationClientCall userConfigurationClientCall;
    @MockBean
    private TagsClientCall tagsClientCall;
    private static final String X_API_KEY_VALUE = "apiKey_value";
    private static final String X_PAGO_PA_SAFESTORAGE_CX_ID_VALUE = "CLIENT_ID_123";
    private static final String DOCUMENT_KEY = "DOCUMENT_KEY";

    private WebTestClient.ResponseSpec additionalFileTagsUpdateTestCall(AdditionalFileTagsUpdateRequest additionalFileTagsUpdateRequest, String documentKey) {

        webTestClient.mutate().responseTimeout(Duration.ofMillis(30000)).build();

        return webTestClient.post()
                .uri(uriBuilder -> uriBuilder.path(PATH_WITH_PARAM).queryParam("documentKey", documentKey).build(documentKey))
                .header(X_PAGOPA_SAFESTORAGE_CX_ID, X_PAGO_PA_SAFESTORAGE_CX_ID_VALUE)
                .header(xApiKey, X_API_KEY_VALUE)
                .header(HttpHeaders.ACCEPT, APPLICATION_JSON_VALUE)
                .bodyValue(additionalFileTagsUpdateRequest)
                .exchange();
    }

    private WebTestClient.ResponseSpec additionalFileTagsMassiveUpdateTestCall(AdditionalFileTagsMassiveUpdateRequest additionalFileTagsMassiveUpdateRequest) {

        webTestClient.mutate().responseTimeout(Duration.ofMillis(30000)).build();

        return webTestClient.post()
                .uri(uriBuilder -> uriBuilder.path(PATH_NO_PARAM).build())
                .header(X_PAGOPA_SAFESTORAGE_CX_ID, X_PAGO_PA_SAFESTORAGE_CX_ID_VALUE)
                .header(xApiKey, X_API_KEY_VALUE)
                .header(HttpHeaders.ACCEPT, APPLICATION_JSON_VALUE)
                .bodyValue(additionalFileTagsMassiveUpdateRequest)
                .exchange();
    }

    @BeforeEach
    public void createUserConfiguration() {
        var userConfiguration =
                new UserConfiguration().name(X_PAGO_PA_SAFESTORAGE_CX_ID_VALUE).apiKey(X_API_KEY_VALUE).canWriteTags(true);
        var userConfigurationResponse = new UserConfigurationResponse().userConfiguration(userConfiguration);
        when(userConfigurationClientCall.getUser(anyString())).thenReturn(Mono.just(userConfigurationResponse));
    }

    /**
     * POST sulla tabella pn-SsTags, update su pn-SsDocuments di una filekey con aggiunta/modifica di differenti tag
     * Risultato atteso: 200 OK
     */
    @Test
    void testSetOk() {
        Map<String, List<String>> set = new HashMap<>();
        set.put("IUN", List.of("XXXFEF3RFD", "CHDGDTFENM"));
        set.put("Conservazione", List.of("2030-12-12"));
        var tag = new AdditionalFileTagsUpdateRequest().SET(set);
        var tagsDto = new TagsDto().tags(set);
        var tagResponse = new TagsResponse().tagsDto(tagsDto);

        when(tagsClientCall.putTags(anyString(), any(TagsChanges.class))).thenReturn(Mono.just(tagResponse));

        additionalFileTagsUpdateTestCall(tag, DOCUMENT_KEY).expectStatus().isOk();
    }

    /**
     * POST sulla tabella pn-SsTags, update su pn-SsDocuments di una filekey con eliminazione di differenti tag
     * Risultato atteso: 200 OK
     */
    @Test
    void testDeleteOk() {
        Map<String, List<String>> delete = new HashMap<>();
        delete.put("Conservazione", List.of("2024-06-27"));
        delete.put("IUN", List.of("IUN1", "IUN2"));
        var tag = new AdditionalFileTagsUpdateRequest().DELETE(delete);
        var tagsDto = new TagsDto().tags(delete);
        var tagResponse = new TagsResponse().tagsDto(tagsDto);

        when(tagsClientCall.putTags(anyString(), any(TagsChanges.class))).thenReturn(Mono.just(tagResponse));

        additionalFileTagsUpdateTestCall(tag, DOCUMENT_KEY).expectStatus().isOk();
    }

    /**
     * POST sulla tabella pn-SsTags, update su pn-SsDocuments di una filekey con aggiunta/modifica ed eliminazione di differenti tag
     * Risultato atteso: 200 OK
     */
    @Test
    void testSetAndDeleteOk() {
        Map<String, List<String>> set = new HashMap<>();
        set.put("IUN", List.of("XXXFEF3RFD", "CHDGDTFENM"));
        Map<String, List<String>> delete = new HashMap<>();
        delete.put("Conservazione", List.of("2024-06-27"));
        var tag = new AdditionalFileTagsUpdateRequest().DELETE(delete).SET(set);
        var tagsDto = new TagsDto().tags(new HashMap<>());
        var tagResponse = new TagsResponse().tagsDto(tagsDto);

        when(tagsClientCall.putTags(anyString(), any(TagsChanges.class))).thenReturn(Mono.just(tagResponse));

        additionalFileTagsUpdateTestCall(tag, DOCUMENT_KEY).expectStatus().isOk();
    }

    /**
     * POST sulla tabella pn-SsTags, update su pn-SsDocuments di una filekey con aggiunta/modifica ed eliminazione dello stesso tag
     * Risultato atteso: 400 BAD REQUEST
     */
    @Test
    void testSetAndDeleteKo() {
        Map<String, List<String>> set = new HashMap<>();
        set.put("Conservazione", List.of("2024-06-29"));
        Map<String, List<String>> delete = new HashMap<>();
        delete.put("Conservazione", List.of("2024-06-27"));
        var tag = new AdditionalFileTagsUpdateRequest().DELETE(delete).SET(set);
        var tagsDto = new TagsDto().tags(new HashMap<>());
        var tagResponse = new TagsResponse().tagsDto(tagsDto);

        when(tagsClientCall.putTags(anyString(), any(TagsChanges.class))).thenReturn(Mono.just(tagResponse));

        additionalFileTagsUpdateTestCall(tag, DOCUMENT_KEY).expectStatus().isBadRequest();
        verify(tagsClientCall, never()).putTags(anyString(), any(TagsChanges.class));
    }

    /**
     * POST sulla tabella pn-SsTags, update su pn-SsDocuments di una filekey con aggiunta di tag singlevalue a cui sono stati associati più valori
     * Risultato atteso: 400 BAD REQUEST
     */
    @Test
    void testSingleValueKo() {
        Map<String, List<String>> set = new HashMap<>();
        set.put("Conservazione", List.of("2024-06-29", "2023-06-22"));
        var tag = new AdditionalFileTagsUpdateRequest().SET(set);
        var tagsDto = new TagsDto().tags(set);
        var tagResponse = new TagsResponse().tagsDto(tagsDto);

        when(tagsClientCall.putTags(anyString(), any(TagsChanges.class))).thenReturn(Mono.just(tagResponse));

        additionalFileTagsUpdateTestCall(tag, DOCUMENT_KEY).expectStatus().isBadRequest();
        verify(tagsClientCall, never()).putTags(anyString(), any(TagsChanges.class));
    }

    /**
     * POST sulla tabella pn-SsTags, update su pn-SsDocuments di una filekey con aggiunta di tag che supera il limite consentito
     * Risultato atteso: 400 BAD REQUEST
     */
    @Test
    void testMaxOperationsOnTagsPerRequestKo() {
        Map<String, List<String>> set = new HashMap<>();
        set.put("Conservazione", List.of("2030-12-12"));
        set.put("IUN", List.of("IUN1", "IUN2"));
        set.put("DataNotifica", List.of("2024-06-29", "2023-06-22"));
        set.put("TAG_MULTIVALUE_NOT_INDEXED", List.of("NONINDEX1", "NONINDEX2"));
        set.put("TAG_SINGLEVALUE_INDEXED", List.of("INDEX1"));

        var tag = new AdditionalFileTagsUpdateRequest().SET(set);
        var tagsDto = new TagsDto().tags(set);
        var tagResponse = new TagsResponse().tagsDto(tagsDto);

        when(tagsClientCall.putTags(anyString(), any(TagsChanges.class))).thenReturn(Mono.just(tagResponse));

        additionalFileTagsUpdateTestCall(tag, DOCUMENT_KEY).expectStatus().isBadRequest();
        verify(tagsClientCall, never()).putTags(anyString(), any(TagsChanges.class));
    }

    /**
     * POST sulla tabella pn-SsTags, update su pn-SsDocuments di una filekey con aggiunta di tag con numero di value che supera il limite consentito
     * Risultato atteso: 400 BAD REQUEST
     */
    @Test
    void testMaxValuesPerTagPerRequestKo() {
        Map<String, List<String>> set = new HashMap<>();
        set.put("IUN", List.of("IUN1", "IUN2", "IUN3", "IUN4", "IUN5", "IUN6"));

        var tag = new AdditionalFileTagsUpdateRequest().SET(set);
        var tagsDto = new TagsDto().tags(set);
        var tagResponse = new TagsResponse().tagsDto(tagsDto);

        when(tagsClientCall.putTags(anyString(), any(TagsChanges.class))).thenReturn(Mono.just(tagResponse));

        additionalFileTagsUpdateTestCall(tag, DOCUMENT_KEY).expectStatus().isBadRequest();
        verify(tagsClientCall, never()).putTags(anyString(), any(TagsChanges.class));
    }

    /**
     * POST sulla tabella pn-SsTags, update su pn-SsDocuments di una filekey con aggiunta di tag da parte di un utente senza permessi
     * Risultato atteso: 403 FORBIDDEN
     */
    @Test
    void testSetForbidden() {
        var userConfiguration =
                new UserConfiguration().name(X_PAGO_PA_SAFESTORAGE_CX_ID_VALUE).apiKey(X_API_KEY_VALUE).canWriteTags(false);
        var userConfigurationResponse = new UserConfigurationResponse().userConfiguration(userConfiguration);
        when(userConfigurationClientCall.getUser(anyString())).thenReturn(Mono.just(userConfigurationResponse));

        Map<String, List<String>> set = new HashMap<>();
        set.put("IUN", List.of("XXXFEF3RFD", "CHDGDTFENM"));
        var tag = new AdditionalFileTagsUpdateRequest().SET(set);
        var tagsDto = new TagsDto().tags(set);
        var tagResponse = new TagsResponse().tagsDto(tagsDto);

        when(tagsClientCall.putTags(anyString(), any(TagsChanges.class))).thenReturn(Mono.just(tagResponse));

        additionalFileTagsUpdateTestCall(tag, DOCUMENT_KEY).expectStatus().isForbidden();
        verify(tagsClientCall, never()).putTags(anyString(), any(TagsChanges.class));
    }

    /**
     * POST sulla tabella pn-SsTags, update su pn-SsDocuments di una fileKey esistente con tag invalido
     * Risultato atteso: 400 BAD REQUEST
     */
    @Test
    void testSetInvalidTag() {
        Map<String, List<String>> set = new HashMap<>();
        set.put("INVALID", List.of("XXXFEF3RFD", "CHDGDTFENM"));
        var tag = new AdditionalFileTagsUpdateRequest().SET(set);
        var tagsDto = new TagsDto().tags(set);
        var tagResponse = new TagsResponse().tagsDto(tagsDto);

        when(tagsClientCall.putTags(anyString(), any(TagsChanges.class))).thenReturn(Mono.just(tagResponse));

        additionalFileTagsUpdateTestCall(tag, DOCUMENT_KEY).expectStatus().isBadRequest();
        verify(tagsClientCall, never()).putTags(anyString(), any(TagsChanges.class));
    }

    /**
     * POST sulla tabella pn-SsTags, update su pn-SsDocuments di una fileKey esistente per eliminazione di un tag invalido
     * Risultato atteso: 400 BAD REQUEST
     */
    @Test
    void testDeleteInvalidTag() {
        Map<String, List<String>> delete = new HashMap<>();
        delete.put("INVALID", List.of("2024-06-27"));
        var tag = new AdditionalFileTagsUpdateRequest().DELETE(delete);
        var tagsDto = new TagsDto().tags(delete);
        var tagResponse = new TagsResponse().tagsDto(tagsDto);

        when(tagsClientCall.putTags(anyString(), any(TagsChanges.class))).thenReturn(Mono.just(tagResponse));

        additionalFileTagsUpdateTestCall(tag, DOCUMENT_KEY).expectStatus().isBadRequest();
        verify(tagsClientCall, never()).putTags(anyString(), any(TagsChanges.class));
    }

    /**
     * POST sulla tabella pn-SsTags, update su pn-SsDocuments di un documento inesistente con relativi tag
     * Risultato atteso: 404 NOT FOUND
     */
    @Test
    void testDocumentNotFound() {
        Map<String, List<String>> set = new HashMap<>();
        set.put("IUN", List.of("XXXFEF3RFD", "CHDGDTFENM"));
        var tag = new AdditionalFileTagsUpdateRequest().SET(set);
        var tagsDto = new TagsDto().tags(set);

        when(tagsClientCall.putTags(anyString(), any(TagsChanges.class))).thenReturn(Mono.error(new DocumentKeyNotPresentException("NOTFOUND")));

        additionalFileTagsUpdateTestCall(tag, "NOTFOUND").expectStatus().isNotFound();
    }

    // UPDATE MASSIVA

    Tags createTagPerRequest (String key) {
        Map<String, List<String>> set = new HashMap<>();
        Map<String, List<String>> delete = new HashMap<>();
        set.put("IUN", List.of("XXXFEF3RFD"));
        set.put("Conservazione", List.of("2030-12-12"));
        delete.put("DataNotifica", List.of("2024-07-18"));
        String fileKey = key;
        return new Tags().fileKey(fileKey).SET(set).DELETE(delete);

    }
    @Test
    void testMassiveRequestOk() {

        List<Tags> tagsList = new ArrayList<>();
        tagsList.add(createTagPerRequest("fileKey"));
        AdditionalFileTagsMassiveUpdateRequest tagsMassiveUpdateRequest = new AdditionalFileTagsMassiveUpdateRequest().tags(tagsList);

        var tagsDto = new TagsDto().tags(tagsMassiveUpdateRequest.getTags().get(0).getSET());
        var tagResponse = new TagsResponse().tagsDto(tagsDto);

        when(tagsClientCall.putTags(anyString(), any(TagsChanges.class))).thenReturn(Mono.just(tagResponse));

        additionalFileTagsMassiveUpdateTestCall(tagsMassiveUpdateRequest).expectStatus().isOk();
    }

    @Test
    void testMassiveRequestKo() {
        List<Tags> tagsList = new ArrayList<>();
        AdditionalFileTagsMassiveUpdateRequest tagsMassiveUpdateRequest = new AdditionalFileTagsMassiveUpdateRequest().tags(tagsList);

        var tagsDto = new TagsDto();
        var tagResponse = new TagsResponse().tagsDto(tagsDto);

        when(tagsClientCall.putTags(anyString(), any(TagsChanges.class))).thenReturn(Mono.just(tagResponse));

        additionalFileTagsMassiveUpdateTestCall(tagsMassiveUpdateRequest).expectStatus().isBadRequest();
        verify(tagsClientCall, never()).putTags(anyString(), any(TagsChanges.class));
    }

    @Test
    void testMassiveRequestDuplicateFileKeyKo() {
        List<Tags> tagsList = new ArrayList<>();
        tagsList.add(createTagPerRequest("fileKey"));
        tagsList.add(createTagPerRequest("fileKey"));
        AdditionalFileTagsMassiveUpdateRequest tagsMassiveUpdateRequest = new AdditionalFileTagsMassiveUpdateRequest().tags(tagsList);

        var tagsDto = new TagsDto().tags(tagsMassiveUpdateRequest.getTags().get(0).getSET());
        var tagResponse = new TagsResponse().tagsDto(tagsDto);

        when(tagsClientCall.putTags(anyString(), any(TagsChanges.class))).thenReturn(Mono.just(tagResponse));

        additionalFileTagsMassiveUpdateTestCall(tagsMassiveUpdateRequest).expectStatus().isBadRequest();
        verify(tagsClientCall, never()).putTags(anyString(), any(TagsChanges.class));

    }

    @Test
    void testMassiveRequestMaxFileKeysUpdateMassivePerRequestKo() {
        List<Tags> tagsList = new ArrayList<>();
        for (int i = 1; i <= 101; i++) {
            tagsList.add(createTagPerRequest("fileKey"+i));
        }

        AdditionalFileTagsMassiveUpdateRequest tagsMassiveUpdateRequest = new AdditionalFileTagsMassiveUpdateRequest().tags(tagsList);

        var tagsDto = new TagsDto().tags(tagsMassiveUpdateRequest.getTags().get(0).getSET());
        var tagResponse = new TagsResponse().tagsDto(tagsDto);

        when(tagsClientCall.putTags(anyString(), any(TagsChanges.class))).thenReturn(Mono.just(tagResponse));

        additionalFileTagsMassiveUpdateTestCall(tagsMassiveUpdateRequest).expectStatus().isBadRequest();
        verify(tagsClientCall, never()).putTags(anyString(), any(TagsChanges.class));
    }

    @Test
    void testMassiveRequestUnauthorizedKo() {

        var userConfiguration =
                new UserConfiguration().name(X_PAGO_PA_SAFESTORAGE_CX_ID_VALUE).apiKey(X_API_KEY_VALUE).canWriteTags(false);
        var userConfigurationResponse = new UserConfigurationResponse().userConfiguration(userConfiguration);
        when(userConfigurationClientCall.getUser(anyString())).thenReturn(Mono.just(userConfigurationResponse));

        List<Tags> tagsList = new ArrayList<>();
        tagsList.add(createTagPerRequest("fileKey"));
        AdditionalFileTagsMassiveUpdateRequest tagsMassiveUpdateRequest = new AdditionalFileTagsMassiveUpdateRequest().tags(tagsList);

        var tagsDto = new TagsDto().tags(tagsMassiveUpdateRequest.getTags().get(0).getSET());
        var tagResponse = new TagsResponse().tagsDto(tagsDto);

        when(tagsClientCall.putTags(anyString(), any(TagsChanges.class))).thenReturn(Mono.just(tagResponse));

        additionalFileTagsMassiveUpdateTestCall(tagsMassiveUpdateRequest).expectStatus().isForbidden();
        verify(tagsClientCall, never()).putTags(anyString(), any(TagsChanges.class));

    }

    @Test
    void testMassiveRequestOkWithSetAndDeleteError() {

        Map<String, List<String>> set = new HashMap<>();
        Map<String, List<String>> delete = new HashMap<>();
        set.put("Conservazione", List.of("2030-12-12"));
        delete.put("Conservazione", List.of("2024-07-18"));
        String fileKey = "documentKey";
        Tags tag = new Tags().fileKey(fileKey).SET(set).DELETE(delete);
        List<Tags> tagsList = new ArrayList<>();
        tagsList.add(tag);
        AdditionalFileTagsMassiveUpdateRequest tagsMassiveUpdateRequest = new AdditionalFileTagsMassiveUpdateRequest().tags(tagsList);

        var tagsDto = new TagsDto().tags(tagsMassiveUpdateRequest.getTags().get(0).getSET());
        var tagResponse = new TagsResponse().tagsDto(tagsDto);

        when(tagsClientCall.putTags(anyString(), any(TagsChanges.class))).thenReturn(Mono.just(tagResponse));

        webTestClient.mutate().responseTimeout(Duration.ofMillis(30000)).build();
        webTestClient.post()
                    .uri(uriBuilder -> uriBuilder.path(PATH_NO_PARAM).build())
                    .header(X_PAGOPA_SAFESTORAGE_CX_ID, X_PAGO_PA_SAFESTORAGE_CX_ID_VALUE)
                    .header(xApiKey, X_API_KEY_VALUE)
                    .header(HttpHeaders.ACCEPT, APPLICATION_JSON_VALUE)
                    .bodyValue(tagsMassiveUpdateRequest)
                    .exchange()
                    .expectStatus()
                    .isOk()
                    .expectBody(AdditionalFileTagsMassiveUpdateResponse.class)
                    .value(AdditionalFileTagsMassiveUpdateResponse::getErrors,
                            Matchers.hasItem(allOf(
                                    hasProperty("resultCode", is("400.00")),
                                    hasProperty("resultDescription", containsStringIgnoringCase("SET and DELETE cannot contain the same tags")),
                                    hasProperty("fileKey", hasItem(containsString("documentKey"))))));

    }

    @Test
    void testMassiveRequestOkWithSingleValueError () {
        Map<String, List<String>> set = new HashMap<>();
        set.put("Conservazione", List.of("2024-07-18", "2030-12-12"));
        String fileKey = "documentKey";
        Tags tag = new Tags().fileKey(fileKey).SET(set);
        List<Tags> tagsList = new ArrayList<>();
        tagsList.add(tag);
        AdditionalFileTagsMassiveUpdateRequest tagsMassiveUpdateRequest = new AdditionalFileTagsMassiveUpdateRequest().tags(tagsList);

        var tagsDto = new TagsDto().tags(tagsMassiveUpdateRequest.getTags().get(0).getSET());
        var tagResponse = new TagsResponse().tagsDto(tagsDto);

        when(tagsClientCall.putTags(anyString(), any(TagsChanges.class))).thenReturn(Mono.just(tagResponse));

        webTestClient.mutate().responseTimeout(Duration.ofMillis(30000)).build();
        webTestClient.post()
                .uri(uriBuilder -> uriBuilder.path(PATH_NO_PARAM).build())
                .header(X_PAGOPA_SAFESTORAGE_CX_ID, X_PAGO_PA_SAFESTORAGE_CX_ID_VALUE)
                .header(xApiKey, X_API_KEY_VALUE)
                .header(HttpHeaders.ACCEPT, APPLICATION_JSON_VALUE)
                .bodyValue(tagsMassiveUpdateRequest)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(AdditionalFileTagsMassiveUpdateResponse.class)
                .value(AdditionalFileTagsMassiveUpdateResponse::getErrors,
                        Matchers.hasItem(allOf(
                                hasProperty("resultCode", is("400.00")),
                                hasProperty("resultDescription", containsStringIgnoringCase("marked as singleValue cannot have multiple values")),
                                hasProperty("fileKey", hasItem(containsString("documentKey"))))));
    }

    @Test
    void testMassiveRequestOkWithMaxOperationsOnTagsPerRequestError() {
        Map<String, List<String>> set = new HashMap<>();
        set.put("Conservazione", List.of("2030-12-12"));
        set.put("IUN", List.of("IUN1", "IUN2"));
        set.put("DataNotifica", List.of("2024-06-29", "2023-06-22"));
        set.put("TAG_MULTIVALUE_NOT_INDEXED", List.of("NONINDEX1", "NONINDEX2"));
        set.put("TAG_SINGLEVALUE_INDEXED", List.of("INDEX1"));
        String fileKey = "documentKey";
        Tags tag = new Tags().fileKey(fileKey).SET(set);
        List<Tags> tagsList = new ArrayList<>();
        tagsList.add(tag);
        AdditionalFileTagsMassiveUpdateRequest tagsMassiveUpdateRequest = new AdditionalFileTagsMassiveUpdateRequest().tags(tagsList);

        var tagsDto = new TagsDto().tags(tagsMassiveUpdateRequest.getTags().get(0).getSET());
        var tagResponse = new TagsResponse().tagsDto(tagsDto);

        when(tagsClientCall.putTags(anyString(), any(TagsChanges.class))).thenReturn(Mono.just(tagResponse));

        webTestClient.mutate().responseTimeout(Duration.ofMillis(30000)).build();
        webTestClient.post()
                .uri(uriBuilder -> uriBuilder.path(PATH_NO_PARAM).build())
                .header(X_PAGOPA_SAFESTORAGE_CX_ID, X_PAGO_PA_SAFESTORAGE_CX_ID_VALUE)
                .header(xApiKey, X_API_KEY_VALUE)
                .header(HttpHeaders.ACCEPT, APPLICATION_JSON_VALUE)
                .bodyValue(tagsMassiveUpdateRequest)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(AdditionalFileTagsMassiveUpdateResponse.class)
                .value(AdditionalFileTagsMassiveUpdateResponse::getErrors,
                        Matchers.hasItem(allOf(
                                hasProperty("resultCode", is("400.00")),
                                hasProperty("resultDescription", containsStringIgnoringCase("Number of tags to update exceeds maxOperationsOnTags limit")),
                                hasProperty("fileKey", hasItem(containsString("documentKey"))))));
    }

    @Test
    void testMassiveRequestOkWithMaxValuesPerTagPerRequestError() {
        Map<String, List<String>> set = new HashMap<>();
        set.put("IUN", List.of("IUN1", "IUN2", "IUN3", "IUN4", "IUN5", "IUN6"));
        String fileKey = "documentKey";
        Tags tag = new Tags().fileKey(fileKey).SET(set);
        List<Tags> tagsList = new ArrayList<>();
        tagsList.add(tag);
        AdditionalFileTagsMassiveUpdateRequest tagsMassiveUpdateRequest = new AdditionalFileTagsMassiveUpdateRequest().tags(tagsList);

        var tagsDto = new TagsDto().tags(tagsMassiveUpdateRequest.getTags().get(0).getSET());
        var tagResponse = new TagsResponse().tagsDto(tagsDto);

        when(tagsClientCall.putTags(anyString(), any(TagsChanges.class))).thenReturn(Mono.just(tagResponse));

        webTestClient.mutate().responseTimeout(Duration.ofMillis(30000)).build();
        webTestClient.post()
                .uri(uriBuilder -> uriBuilder.path(PATH_NO_PARAM).build())
                .header(X_PAGOPA_SAFESTORAGE_CX_ID, X_PAGO_PA_SAFESTORAGE_CX_ID_VALUE)
                .header(xApiKey, X_API_KEY_VALUE)
                .header(HttpHeaders.ACCEPT, APPLICATION_JSON_VALUE)
                .bodyValue(tagsMassiveUpdateRequest)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(AdditionalFileTagsMassiveUpdateResponse.class)
                .value(AdditionalFileTagsMassiveUpdateResponse::getErrors,
                        Matchers.hasItem(allOf(
                                hasProperty("resultCode", is("400.00")),
                                hasProperty("resultDescription", containsStringIgnoringCase("Number of values for tag ")),
                                hasProperty("fileKey", hasItem(containsString("documentKey"))))));
    }

    @Test
    void testMassiveRequestOkWithSetInvalidTagError() {
        Map<String, List<String>> set = new HashMap<>();
        set.put("INVALID", List.of("XXXFEF3RFD", "CHDGDTFENM"));
        String fileKey = "documentKey";
        Tags tag = new Tags().fileKey(fileKey).SET(set);
        List<Tags> tagsList = new ArrayList<>();
        tagsList.add(tag);
        AdditionalFileTagsMassiveUpdateRequest tagsMassiveUpdateRequest = new AdditionalFileTagsMassiveUpdateRequest().tags(tagsList);

        var tagsDto = new TagsDto().tags(tagsMassiveUpdateRequest.getTags().get(0).getSET());
        var tagResponse = new TagsResponse().tagsDto(tagsDto);

        when(tagsClientCall.putTags(anyString(), any(TagsChanges.class))).thenReturn(Mono.just(tagResponse));

        webTestClient.mutate().responseTimeout(Duration.ofMillis(30000)).build();
        webTestClient.post()
                .uri(uriBuilder -> uriBuilder.path(PATH_NO_PARAM).build())
                .header(X_PAGOPA_SAFESTORAGE_CX_ID, X_PAGO_PA_SAFESTORAGE_CX_ID_VALUE)
                .header(xApiKey, X_API_KEY_VALUE)
                .header(HttpHeaders.ACCEPT, APPLICATION_JSON_VALUE)
                .bodyValue(tagsMassiveUpdateRequest)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(AdditionalFileTagsMassiveUpdateResponse.class)
                .value(AdditionalFileTagsMassiveUpdateResponse::getErrors,
                        Matchers.hasItem(allOf(
                                hasProperty("resultCode", is("400.00")),
                                hasProperty("resultDescription", containsStringIgnoringCase("not found in the indexing configuration")),
                                hasProperty("fileKey", hasItem(containsString("documentKey"))))));
    }

    @Test
    void testMassiveRequestOkWithDeleteInvalidTagError() {
        Map<String, List<String>> delete = new HashMap<>();
        delete.put("INVALID", List.of("XXXFEF3RFD", "CHDGDTFENM"));
        String fileKey = "documentKey";
        Tags tag = new Tags().fileKey(fileKey).DELETE(delete);
        List<Tags> tagsList = new ArrayList<>();
        tagsList.add(tag);
        AdditionalFileTagsMassiveUpdateRequest tagsMassiveUpdateRequest = new AdditionalFileTagsMassiveUpdateRequest().tags(tagsList);

        var tagsDto = new TagsDto().tags(tagsMassiveUpdateRequest.getTags().get(0).getSET());
        var tagResponse = new TagsResponse().tagsDto(tagsDto);

        when(tagsClientCall.putTags(anyString(), any(TagsChanges.class))).thenReturn(Mono.just(tagResponse));

        webTestClient.mutate().responseTimeout(Duration.ofMillis(30000)).build();
        webTestClient.post()
                .uri(uriBuilder -> uriBuilder.path(PATH_NO_PARAM).build())
                .header(X_PAGOPA_SAFESTORAGE_CX_ID, X_PAGO_PA_SAFESTORAGE_CX_ID_VALUE)
                .header(xApiKey, X_API_KEY_VALUE)
                .header(HttpHeaders.ACCEPT, APPLICATION_JSON_VALUE)
                .bodyValue(tagsMassiveUpdateRequest)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(AdditionalFileTagsMassiveUpdateResponse.class)
                .value(AdditionalFileTagsMassiveUpdateResponse::getErrors,
                        Matchers.hasItem(allOf(
                                hasProperty("resultCode", is("400.00")),
                                hasProperty("resultDescription", containsStringIgnoringCase("not found in the indexing configuration")),
                                hasProperty("fileKey", hasItem(containsString("documentKey"))))));
    }

}
