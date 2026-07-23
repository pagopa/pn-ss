package it.pagopa.pnss.indexing.service.impl;

import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.TagsChanges;
import it.pagopa.pnss.common.exception.RequestValidationException;
import it.pagopa.pnss.common.model.pojo.IndexingLimits;
import it.pagopa.pnss.common.model.pojo.IndexingTag;
import it.pagopa.pnss.configuration.IndexingConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit test for the shared tag-preparation method used by the createFile flow.
 * Builds a real {@link IndexingConfiguration} (no Spring/SSM) and populates its tag map directly,
 * so it can run headless without Testcontainers.
 */
class AdditionalFileTagsServiceImplTest {

    private static final String GLOBAL_TAG = "IUN";
    private static final String LOCAL_CLIENT = "pn-radd-fsu";
    private static final String LOCAL_TAG = "DataCreazione";
    private static final String LOCAL_TAG_KEY = LOCAL_CLIENT + "~" + LOCAL_TAG;
    private static final String SINGLE_VALUE_CLIENT = "pn-downtime-logs";
    private static final String SINGLE_VALUE_TAG = "active";
    private static final String SINGLE_VALUE_TAG_KEY = SINGLE_VALUE_CLIENT + "~" + SINGLE_VALUE_TAG;

    private AdditionalFileTagsServiceImpl additionalFileTagsService;

    @BeforeEach
    void setUp() {
        IndexingConfiguration indexingConfiguration = new IndexingConfiguration(null, null, null);
        indexingConfiguration.getTags().put(GLOBAL_TAG,
                IndexingTag.builder().key(GLOBAL_TAG).indexed(true).multivalue(true).global(true).build());
        indexingConfiguration.getTags().put(LOCAL_TAG_KEY,
                IndexingTag.builder().key(LOCAL_TAG_KEY).indexed(true).multivalue(true).global(false).build());
        indexingConfiguration.getTags().put(SINGLE_VALUE_TAG_KEY,
                IndexingTag.builder().key(SINGLE_VALUE_TAG_KEY).indexed(false).multivalue(false).global(false).build());

        // I limiti non vengono popolati da init() (no Spring/SSM in questo unit test): li impostiamo
        // esplicitamente perche' il metodo condiviso di preparazione applica anche MaxValuesPerTagPerRequest.
        ReflectionTestUtils.setField(indexingConfiguration, "indexingLimits",
                IndexingLimits.builder()
                        .maxTagsPerRequest(50L)
                        .maxOperationsOnTagsPerRequest(50L)
                        .maxFileKeys(1000L)
                        .maxMapValuesForSearch(10L)
                        .maxFileKeysUpdateMassivePerRequest(100L)
                        .maxTagsPerDocument(40L)
                        .maxValuesPerTagDocument(1000L)
                        .maxValuesPerTagPerRequest(100L)
                        .build());

        additionalFileTagsService = new AdditionalFileTagsServiceImpl(null, indexingConfiguration, null, null, null);
    }

    @Test
    void validateTagsForFileCreation_localTag_isNamespacedWithCxId() {
        Map<String, List<String>> setTags = Map.of(LOCAL_TAG, List.of("2024-01-01"));

        TagsChanges result = additionalFileTagsService.validateTagsForFileCreation(setTags, LOCAL_CLIENT).block();

        assertTrue(result.getSET().containsKey(LOCAL_TAG_KEY));
        assertFalse(result.getSET().containsKey(LOCAL_TAG));
        assertTrue(result.getDELETE() == null || result.getDELETE().isEmpty());
    }

    @Test
    void validateTagsForFileCreation_globalTag_isKeptAsIs() {
        Map<String, List<String>> setTags = Map.of(GLOBAL_TAG, List.of("TEST-IUN-001"));

        TagsChanges result = additionalFileTagsService.validateTagsForFileCreation(setTags, "any-cx-id").block();

        assertTrue(result.getSET().containsKey(GLOBAL_TAG));
    }

    @Test
    void validateTagsForFileCreation_nonExistentTag_throwsRequestValidationException() {
        Map<String, List<String>> setTags = Map.of("TagInesistenteXYZ", List.of("v"));

        assertThrows(RequestValidationException.class,
                () -> additionalFileTagsService.validateTagsForFileCreation(setTags, "any-cx-id").block());
    }

    @Test
    void validateTagsForFileCreation_singleValueTagWithMultipleValues_throwsRequestValidationException() {
        Map<String, List<String>> setTags = Map.of(SINGLE_VALUE_TAG, List.of("true", "false"));

        assertThrows(RequestValidationException.class,
                () -> additionalFileTagsService.validateTagsForFileCreation(setTags, SINGLE_VALUE_CLIENT).block());
    }
}
