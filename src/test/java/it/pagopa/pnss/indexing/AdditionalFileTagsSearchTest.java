package it.pagopa.pnss.indexing;

import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.*;
import it.pagopa.pnss.common.client.DocumentClientCall;
import it.pagopa.pnss.common.client.TagsClientCall;
import it.pagopa.pnss.common.client.UserConfigurationClientCall;
import it.pagopa.pnss.common.client.exception.TagKeyValueNotPresentException;
import it.pagopa.pnss.testutils.annotation.SpringBootTestWebEnv;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.when;

@SpringBootTestWebEnv
@AutoConfigureWebTestClient(timeout = "100000")
class AdditionalFileTagsSearchTest {

    @MockBean
    private TagsClientCall tagsClientCall;
    @MockBean
    private UserConfigurationClientCall userConfigurationClientCall;
    @MockBean
    private DocumentClientCall documentClientCall;
    @Autowired
    private WebTestClient webTestClient;
    private static final String SEARCH_URI = "/safe-storage/v1/files/tags";
    private static final String FILE_KEY = "SearchTestFileKey";


    // Nomi e apiKey di client utilizzati per i test
    private static final String PN_CLIENT_AUTHORIZED = "pn-client-authorized";
    private static final String PN_CLIENT_AUTHORIZED_API_KEY = "pn-client-authorized_api_key";
    private static final String PN_CLIENT_NOT_AUTHORIZED = "pn-client-not-authorized";
    private static final String PN_CLIENT_NOT_AUTHORIZED_API_KEY = "pn-client-not-authorized_api_key";


    // Chiavi dei tag utilizzati per i test
    private static final String IUN = "IUN";
    private static final String CONSERVAZIONE = "Conservazione";

    @BeforeEach
    void beforeEach() {
        // Client with authorization to read tags
        when(userConfigurationClientCall.getUser(PN_CLIENT_AUTHORIZED)).thenReturn(Mono.just(new UserConfigurationResponse().userConfiguration(new UserConfiguration().name(PN_CLIENT_AUTHORIZED).apiKey(PN_CLIENT_AUTHORIZED_API_KEY).canReadTags(true))));
        // Client NOT authorized to read tags
        when(userConfigurationClientCall.getUser(PN_CLIENT_NOT_AUTHORIZED)).thenReturn(Mono.just(new UserConfigurationResponse().userConfiguration(new UserConfiguration().name(PN_CLIENT_NOT_AUTHORIZED).apiKey(PN_CLIENT_NOT_AUTHORIZED_API_KEY).canReadTags(false))));
    }

    private WebTestClient.ResponseSpec additionalFileTagsSearchCall(String logic, Boolean tags, String xPagopaSafestorageCxId, String xApiKey, Map<String, String> tagsToSearch) {
        MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();

        if (logic != null) {
            queryParams.add("logic", logic);
        }

        if (tags != null) {
            queryParams.add("tags", tags.toString());
        }

        if (tagsToSearch != null) {
            for (Map.Entry<String, String> tag : tagsToSearch.entrySet()) {
                queryParams.add(tag.getKey(), tag.getValue());
            }
        }

        return webTestClient.get()
                .uri(uriBuilder -> uriBuilder.path(SEARCH_URI).queryParams(queryParams).build())
                .accept(MediaType.APPLICATION_JSON)
                .header("x-pagopa-safestorage-cx-id", xPagopaSafestorageCxId)
                .header("x-api-key", xApiKey)
                .exchange();
    }

    @SafeVarargs
    private WebTestClient.ResponseSpec additionalFileTagsSearchCall(String logic, Boolean tags, String xPagopaSafestorageCxId, String xApiKey, Map.Entry<String, String>... tagsToSearch) {
        return additionalFileTagsSearchCall(logic, tags, xPagopaSafestorageCxId, xApiKey, Map.ofEntries(tagsToSearch));
    }

    /**
     * Logica OR
     * Ricerca di file con tag IUN=ABCDEF o Conservazione=OK
     * Due casi:
     * - IUN matcha, Conservazione non matcha
     * - IUN non matcha, Conservazione matcha
     * Entrambi i casi restituiscono 200 OK con la fileKey corretta
     */
    @ParameterizedTest
    @ValueSource(strings = {"IUN", "Conservazione"})
    void search_orLogic_ok(String tagToMatch) {

        //GIVEN
        String iunValue = "ABCDEF";
        String conservazioneValue = "OK";
        String iunKeyValue = IUN + "~" + iunValue;
        String conservazioneKeyValue = CONSERVAZIONE + "~" + conservazioneValue;

        //WHEN
        if (tagToMatch.equals(IUN)) {
            // Il tag IUN matcha, il tag Conservazione no
            when(tagsClientCall.getTagsRelations(iunKeyValue)).thenReturn(Mono.just(new TagsRelationsResponse().tagsRelationsDto(new TagsRelationsDto().tagKeyValue(iunKeyValue).addFileKeysItem(FILE_KEY))));
            when(tagsClientCall.getTagsRelations(conservazioneKeyValue)).thenReturn(Mono.error(new TagKeyValueNotPresentException(conservazioneKeyValue)));
        } else {
            // Il tag Conservazione matcha, il tag IUN no
            when(tagsClientCall.getTagsRelations(iunKeyValue)).thenReturn(Mono.error(new TagKeyValueNotPresentException(iunKeyValue)));
            when(tagsClientCall.getTagsRelations(conservazioneKeyValue)).thenReturn(Mono.just(new TagsRelationsResponse().tagsRelationsDto(new TagsRelationsDto().tagKeyValue(conservazioneKeyValue).addFileKeysItem(FILE_KEY))));
        }

        //THEN
        additionalFileTagsSearchCall("or", false, PN_CLIENT_AUTHORIZED, PN_CLIENT_AUTHORIZED_API_KEY, Map.entry(IUN, iunValue), Map.entry(CONSERVAZIONE, conservazioneValue))
                .expectStatus()
                .isOk()
                .expectBody(AdditionalFileTagsSearchResponse.class)
                .value(response -> {
                    assertThat(response.getFileKeys(), notNullValue());
                    assertThat(response.getFileKeys(), hasSize(1));
                    assertThat(response.getFileKeys(), hasItem(hasProperty("fileKey", equalTo(FILE_KEY))));
                    assertThat(response.getFileKeys(), hasItem(hasProperty("tags", nullValue())));
                });
    }

    /**
     * Logica OR
     * Ricerca di file con tag IUN=ABCDEF
     * Risultato atteso: 200 OK con la fileKey corretta
     */
    @Test
    void search_orLogic_SingleTag_ok() {

        //GIVEN
        String iunValue = "ABCDEF";
        String iunKeyValue = IUN + "~" + iunValue;

        //WHEN
        when(tagsClientCall.getTagsRelations(iunKeyValue)).thenReturn(Mono.just(new TagsRelationsResponse().tagsRelationsDto(new TagsRelationsDto().tagKeyValue(iunKeyValue).addFileKeysItem(FILE_KEY))));


        //THEN
        additionalFileTagsSearchCall("or", false, PN_CLIENT_AUTHORIZED, PN_CLIENT_AUTHORIZED_API_KEY, Map.entry(IUN, iunValue))
                .expectStatus()
                .isOk()
                .expectBody(AdditionalFileTagsSearchResponse.class)
                .value(response -> {
                    assertThat(response.getFileKeys(), notNullValue());
                    assertThat(response.getFileKeys(), hasSize(1));
                    assertThat(response.getFileKeys(), hasItem(hasProperty("fileKey", equalTo(FILE_KEY))));
                    assertThat(response.getFileKeys(), hasItem(hasProperty("tags", nullValue())));
                });
    }

    /**
     * Logica OR.
     * Ricerca di file con tag IUN=ABCDEF o Conservazione=OK, senza alcun match.
     * Risultato atteso: 200 OK con lista fileKey vuota.
     */
    @Test
    void search_orLogic_noResult() {

        //GIVEN
        String iunValue = "ABCDEF";
        String conservazioneValue = "OK";
        String iunKeyValue = IUN + "~" + iunValue;
        String conservazioneKeyValue = CONSERVAZIONE + "~" + conservazioneValue;

        //WHEN
        when(tagsClientCall.getTagsRelations(conservazioneKeyValue)).thenReturn(Mono.error(new TagKeyValueNotPresentException(conservazioneKeyValue)));
        when(tagsClientCall.getTagsRelations(iunKeyValue)).thenReturn(Mono.error(new TagKeyValueNotPresentException(iunKeyValue)));

        //THEN
        additionalFileTagsSearchCall("or", false, PN_CLIENT_AUTHORIZED, PN_CLIENT_AUTHORIZED_API_KEY, Map.entry(IUN, iunValue), Map.entry(CONSERVAZIONE, conservazioneValue))
                .expectStatus()
                .isOk()
                .expectBody(AdditionalFileTagsSearchResponse.class)
                .value(response -> {
                    assertThat(response.getFileKeys(), notNullValue());
                    assertThat(response.getFileKeys(), empty());
                });
    }

    /**
     * Logica AND. Testiamo anche il valore di default per il query param "logic"
     * Ricerca di file con tag IUN=ABCDEF e Conservazione=OK
     * Risultato atteso: 200 OK con la fileKey corretta
     */
    @ParameterizedTest
    @ValueSource(strings = {"and", ""})
    void search_andLogic_ok(String logicValue) {

        //GIVEN
        logicValue = logicValue.isEmpty() ? null : logicValue;
        String iunValue = "ABCDEF";
        String conservazioneValue = "OK";
        String iunKeyValue = IUN + "~" + iunValue;
        String conservazioneKeyValue = CONSERVAZIONE + "~" + conservazioneValue;

        //WHEN
        when(tagsClientCall.getTagsRelations(iunKeyValue)).thenReturn(Mono.just(new TagsRelationsResponse().tagsRelationsDto(new TagsRelationsDto().tagKeyValue(iunKeyValue).addFileKeysItem(FILE_KEY))));
        when(tagsClientCall.getTagsRelations(conservazioneKeyValue)).thenReturn(Mono.just(new TagsRelationsResponse().tagsRelationsDto(new TagsRelationsDto().tagKeyValue(conservazioneKeyValue).addFileKeysItem(FILE_KEY))));

        //THEN
        additionalFileTagsSearchCall(logicValue, false, PN_CLIENT_AUTHORIZED, PN_CLIENT_AUTHORIZED_API_KEY, Map.entry(IUN, iunValue), Map.entry(CONSERVAZIONE, conservazioneValue))
                .expectStatus()
                .isOk()
                .expectBody(AdditionalFileTagsSearchResponse.class)
                .value(response -> {
                    assertThat(response.getFileKeys(), notNullValue());
                    assertThat(response.getFileKeys(), hasSize(1));
                    assertThat(response.getFileKeys(), hasItem(hasProperty("fileKey", equalTo(FILE_KEY))));
                    assertThat(response.getFileKeys(), hasItem(hasProperty("tags", nullValue())));
                });
    }

    /**
     * Logica AND.
     * Ricerca di file con tag IUN=ABCDEF
     * Risultato atteso: 200 OK con la fileKey corretta
     */
    @Test
    void search_andLogic_SingleTag_ok() {

        //GIVEN
        String iunValue = "ABCDEF";
        String iunKeyValue = IUN + "~" + iunValue;

        //WHEN
        when(tagsClientCall.getTagsRelations(iunKeyValue)).thenReturn(Mono.just(new TagsRelationsResponse().tagsRelationsDto(new TagsRelationsDto().tagKeyValue(iunKeyValue).addFileKeysItem(FILE_KEY))));

        //THEN
        additionalFileTagsSearchCall("and", false, PN_CLIENT_AUTHORIZED, PN_CLIENT_AUTHORIZED_API_KEY, Map.entry(IUN, iunValue))
                .expectStatus()
                .isOk()
                .expectBody(AdditionalFileTagsSearchResponse.class)
                .value(response -> {
                    assertThat(response.getFileKeys(), notNullValue());
                    assertThat(response.getFileKeys(), hasSize(1));
                    assertThat(response.getFileKeys(), hasItem(hasProperty("fileKey", equalTo(FILE_KEY))));
                    assertThat(response.getFileKeys(), hasItem(hasProperty("tags", nullValue())));
                });
    }

    /**
     * Logica AND.
     * Ricerca di file con tag IUN=ABCDEF e Conservazione=OK
     * Risultato atteso: 200 OK con la fileKey corretta
     */
    @Test
    void search_andLogic_noResult() {

        //GIVEN
        String iunValue = "ABCDEF";
        String conservazioneValue = "OK";
        String iunKeyValue = IUN + "~" + iunValue;
        String conservazioneKeyValue = CONSERVAZIONE + "~" + conservazioneValue;

        //WHEN
        when(tagsClientCall.getTagsRelations(iunKeyValue)).thenReturn(Mono.just(new TagsRelationsResponse().tagsRelationsDto(new TagsRelationsDto().tagKeyValue(iunKeyValue).addFileKeysItem(FILE_KEY))));
        when(tagsClientCall.getTagsRelations(conservazioneKeyValue)).thenReturn(Mono.error(new TagKeyValueNotPresentException(conservazioneKeyValue)));

        //THEN
        additionalFileTagsSearchCall("and", false, PN_CLIENT_AUTHORIZED, PN_CLIENT_AUTHORIZED_API_KEY, Map.entry(IUN, iunValue), Map.entry(CONSERVAZIONE, conservazioneValue))
                .expectStatus()
                .isOk()
                .expectBody(AdditionalFileTagsSearchResponse.class)
                .value(response -> {
                    assertThat(response.getFileKeys(), notNullValue());
                    assertThat(response.getFileKeys(), empty());
                });
    }

    /**
     * Query param "logic" non valido.
     * Risultato atteso: 400 BAD REQUEST
     */
    @Test
    void search_badLogic_noResult() {

        //GIVEN
        String iunValue = "ABCDEF";
        String conservazioneValue = "OK";
        String iunKeyValue = IUN + "~" + iunValue;
        String conservazioneKeyValue = CONSERVAZIONE + "~" + conservazioneValue;

        //WHEN
        when(tagsClientCall.getTagsRelations(iunKeyValue)).thenReturn(Mono.just(new TagsRelationsResponse().tagsRelationsDto(new TagsRelationsDto().tagKeyValue(iunKeyValue).addFileKeysItem(FILE_KEY))));
        when(tagsClientCall.getTagsRelations(conservazioneKeyValue)).thenReturn(Mono.error(new TagKeyValueNotPresentException(conservazioneKeyValue)));

        //THEN
        additionalFileTagsSearchCall("bad", false, PN_CLIENT_AUTHORIZED, PN_CLIENT_AUTHORIZED_API_KEY, Map.entry(IUN, iunValue), Map.entry(CONSERVAZIONE, conservazioneValue))
                .expectStatus()
                .isBadRequest();
    }

    /**
     * Test del valore di default per il query param "tags"
     * Ricerca di file con tag IUN=ABCDEF e Conservazione=OK
     * Risultato atteso: 200 OK con la fileKey corretta
     */
    @Test
    void search_tagsDefaultValue_ok() {

        //GIVEN
        String iunValue = "ABCDEF";
        String conservazioneValue = "OK";
        String iunKeyValue = IUN + "~" + iunValue;
        String conservazioneKeyValue = CONSERVAZIONE + "~" + conservazioneValue;

        //WHEN
        when(tagsClientCall.getTagsRelations(iunKeyValue)).thenReturn(Mono.just(new TagsRelationsResponse().tagsRelationsDto(new TagsRelationsDto().tagKeyValue(iunKeyValue).addFileKeysItem(FILE_KEY))));
        when(tagsClientCall.getTagsRelations(conservazioneKeyValue)).thenReturn(Mono.just(new TagsRelationsResponse().tagsRelationsDto(new TagsRelationsDto().tagKeyValue(conservazioneKeyValue).addFileKeysItem(FILE_KEY))));

        //THEN
        additionalFileTagsSearchCall("and", null, PN_CLIENT_AUTHORIZED, PN_CLIENT_AUTHORIZED_API_KEY, Map.entry(IUN, iunValue), Map.entry(CONSERVAZIONE, conservazioneValue))
                .expectStatus()
                .isOk()
                .expectBody(AdditionalFileTagsSearchResponse.class)
                .value(response -> {
                    assertThat(response.getFileKeys(), notNullValue());
                    assertThat(response.getFileKeys(), hasSize(1));
                    assertThat(response.getFileKeys(), hasItem(hasProperty("fileKey", equalTo(FILE_KEY))));
                    assertThat(response.getFileKeys(), hasItem(hasProperty("tags", nullValue())));
                });
    }

    /**
     * Test con il valore per il query param "tags" a true
     * Ricerca di file con tag IUN=ABCDEF e Conservazione=OK
     * Risultato atteso: 200 OK con la fileKey corretta e i relativi tag.
     */
    @Test
    void search_tagsTrue_ok() {

        //GIVEN
        String iunValue = "ABCDEF";
        String conservazioneValue = "OK";
        String iunKeyValue = IUN + "~" + iunValue;
        String conservazioneKeyValue = CONSERVAZIONE + "~" + conservazioneValue;

        //WHEN
        when(tagsClientCall.getTagsRelations(iunKeyValue)).thenReturn(Mono.just(new TagsRelationsResponse().tagsRelationsDto(new TagsRelationsDto().tagKeyValue(iunKeyValue).addFileKeysItem(FILE_KEY))));
        when(tagsClientCall.getTagsRelations(conservazioneKeyValue)).thenReturn(Mono.just(new TagsRelationsResponse().tagsRelationsDto(new TagsRelationsDto().tagKeyValue(conservazioneKeyValue).addFileKeysItem(FILE_KEY))));
        when(documentClientCall.getDocument(FILE_KEY)).thenReturn(Mono.just(new DocumentResponse().document(new Document().tags(Map.of(IUN, List.of(iunValue), CONSERVAZIONE, List.of(conservazioneValue))))));

        //THEN
        additionalFileTagsSearchCall("and", true, PN_CLIENT_AUTHORIZED, PN_CLIENT_AUTHORIZED_API_KEY, Map.entry(IUN, iunValue), Map.entry(CONSERVAZIONE, conservazioneValue))
                .expectStatus()
                .isOk()
                .expectBody(AdditionalFileTagsSearchResponse.class)
                .value(response -> {
                    assertThat(response.getFileKeys(), notNullValue());
                    assertThat(response.getFileKeys(), hasSize(1));
                    assertThat(response.getFileKeys(), hasItem(hasProperty("fileKey", equalTo(FILE_KEY))));
                    assertThat(response.getFileKeys(), hasItem(hasProperty("tags", notNullValue())));
                });
    }

    /**
     * Operazione di search con un client non autorizzato in lettura.
     * Risultato atteso: 403 KO.
     */
    @Test
    void search_Forbidden_Ko() {
        additionalFileTagsSearchCall("and", false, PN_CLIENT_NOT_AUTHORIZED, PN_CLIENT_NOT_AUTHORIZED_API_KEY)
                .expectStatus()
                .isForbidden();
    }

    /**
     * Test con numero di tag che superano il limite preimpostato (10)
     * Risultato atteso: 400 BAD REQUEST
     */
    @Test
    void search_MaxMapValuesForSearch_Ko() {
        HashMap<String, String> tags = new HashMap<>();
        for (int i = 0; i <= 10; i++) {
            tags.put("key" + i, "value" + i);
        }
        additionalFileTagsSearchCall("and", false, PN_CLIENT_AUTHORIZED, PN_CLIENT_AUTHORIZED_API_KEY, tags)
                .expectStatus()
                .isBadRequest();
    }

}
