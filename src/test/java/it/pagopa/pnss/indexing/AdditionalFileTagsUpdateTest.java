package it.pagopa.pnss.indexing;

import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.*;
import it.pagopa.pnss.common.client.DocumentClientCall;
import it.pagopa.pnss.common.client.TagsClientCall;
import it.pagopa.pnss.common.client.UserConfigurationClientCall;
import it.pagopa.pnss.common.client.exception.DocumentKeyNotPresentException;
import it.pagopa.pnss.indexing.service.AdditionalFileTagsService;
import it.pagopa.pnss.testutils.annotation.SpringBootTestWebEnv;
import lombok.CustomLog;
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

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    @MockBean
    private UserConfigurationClientCall userConfigurationClientCall;
    @MockBean
    private TagsClientCall tagsClientCall;
    @MockBean
    private DocumentClientCall documentClientCall;
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

    @BeforeEach
    public void createUserConfiguration() {
        var userConfiguration =
                new UserConfiguration().name(X_PAGO_PA_SAFESTORAGE_CX_ID_VALUE).apiKey(X_API_KEY_VALUE).canWriteTags(true);
        var userConfigurationResponse = new UserConfigurationResponse().userConfiguration(userConfiguration);
        when(userConfigurationClientCall.getUser(anyString())).thenReturn(Mono.just(userConfigurationResponse));
    }

    @BeforeEach
    public void createDocumentClientCall(){
        var documentConfiguration = new Document();
        var documentResponse = new DocumentResponse().document(documentConfiguration);
        when(documentClientCall.getDocument(anyString())).thenReturn(Mono.just(documentResponse));
    }

    /**
     * POST sulla tabella pn-SsTags di una filekey con aggiunta/modifica di differenti tag
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
     * POST sulla tabella pn-SsTags di una filekey con eliminazione di differenti tag
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
     * POST sulla tabella pn-SsTags di una filekey con aggiunta/modifica ed eliminazione di differenti tag
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
     * POST sulla tabella pn-SsTags di una filekey con aggiunta/modifica ed eliminazione dello stesso tag
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
     * POST sulla tabella pn-SsTags di una filekey con aggiunta di tag singlevalue a cui sono stati associati più valori
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
     * POST sulla tabella pn-SsTags di una filekey con aggiunta di tag con numero di value che supera il limite consentito
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
     * POST sulla tabella pn-SsTags di una filekey con aggiunta di tag con numero di value che supera il limite consentito
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
     * POST sulla tabella pn-SsTags di una filekey con aggiunta di tag da parte di un utente senza permessi
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
     * POST sulla tabella pn-SsTags di una fileKey esistente con tag invalido
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
     * POST sulla tabella pn-SsTags di una fileKey esistente per eliminazione di un tag invalido
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
     * POST sulla tabella pn-SsTags di un documento inesistente con relativi tag
     * Risultato atteso: 404 NOT FOUND
     */
    @Test
    void testDocumentNotFound() {
        Map<String, List<String>> set = new HashMap<>();
        set.put("IUN", List.of("XXXFEF3RFD", "CHDGDTFENM"));
        var tag = new AdditionalFileTagsUpdateRequest().SET(set);
        var tagsDto = new TagsDto().tags(set);
        var tagResponse = new TagsResponse().tagsDto(tagsDto);

        when(tagsClientCall.putTags(anyString(), any(TagsChanges.class))).thenReturn(Mono.just(tagResponse));
        when(documentClientCall.getDocument("NOTFOUND")).thenReturn(Mono.error(new DocumentKeyNotPresentException("NOTFOUND")));

        additionalFileTagsUpdateTestCall(tag, "NOTFOUND").expectStatus().isNotFound();
        verify(tagsClientCall, never()).putTags(anyString(), any(TagsChanges.class));
    }

}
