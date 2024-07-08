package it.pagopa.pnss.repositorymanager.rest.internal;

import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.TagsChanges;
import it.pagopa.pnss.configurationproperties.RepositoryManagerDynamoTableName;
import it.pagopa.pnss.repositorymanager.entity.DocumentEntity;
import it.pagopa.pnss.repositorymanager.entity.TagsEntity;
import it.pagopa.pnss.testutils.annotation.SpringBootTestWebEnv;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.reactive.server.WebTestClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

@SpringBootTestWebEnv
class TagsInternalApiControllerTest {

    private static DynamoDbTable<TagsEntity> tagsEntityDynamoDbAsyncTable;
    private static DynamoDbTable<DocumentEntity> documentEntityDynamoDbAsyncTable;
    private static final String PUT_TAGS_PATH = "/safestorage/internal/v1/tags";

    @BeforeAll
    public static void insertDefaultDocument(@Autowired DynamoDbEnhancedClient dynamoDbEnhancedClient,
                                             @Autowired RepositoryManagerDynamoTableName repositoryManagerDynamoTableName) {
        tagsEntityDynamoDbAsyncTable = dynamoDbEnhancedClient.table(repositoryManagerDynamoTableName.tagsName(), TableSchema.fromBean(TagsEntity.class));
        documentEntityDynamoDbAsyncTable = dynamoDbEnhancedClient.table(repositoryManagerDynamoTableName.documentiName(), TableSchema.fromBean(DocumentEntity.class));
    }

    @Autowired
    private WebTestClient webTestClient;

    @Nested
    class PutTagsTest {
        private static final String PARTITION_ID = "DocumentKeyPutTagsTest";
        private static final String IUN = "IUN";
        private static final String CONSERVAZIONE = "Conservazione";
        private static final String TAG_MULTIVALUE_NOT_INDEXED = "TAG_MULTIVALUE_NOT_INDEXED";

        // Serie di test su un documento senza tag sulla pn-SsDocumenti o associazioni sulla pn-SsTags (documento pulito)
        @Nested
        class DocumentWithNoTags {
            @BeforeEach
            void beforeEach() {
                DocumentEntity documentEntity = new DocumentEntity();
                documentEntity.setDocumentKey(PARTITION_ID);
                documentEntityDynamoDbAsyncTable.putItem(documentEntityBuilder -> documentEntityBuilder.item(documentEntity));
            }

            @AfterEach
            void afterEach() {
                // Tables clean-up
                documentEntityDynamoDbAsyncTable.scan().stream().forEach(documentEntityPage -> documentEntityPage.items().forEach(documentEntity -> documentEntityDynamoDbAsyncTable.deleteItem(documentEntity)));
                tagsEntityDynamoDbAsyncTable.scan().stream().forEach(tagsEntityPage -> tagsEntityPage.items().forEach(tagsEntity -> tagsEntityDynamoDbAsyncTable.deleteItem(tagsEntity)));
            }

            /**
             * Inserimento di un singolo valore su un tag multivalue e indicizzato.
             * Risultato atteso: 200 OK
             */
            @Test
            void putTags_Set_Indexed_OneValue_Ok() {
                String tagValue = "ABCDEF";
                Map<String, List<String>> setTags = Map.of(IUN, List.of(tagValue));
                webTestClient.put().uri(PUT_TAGS_PATH)
                        .bodyValue(new TagsChanges().fileKey(PARTITION_ID).SET(setTags))
                        .exchange()
                        .expectStatus()
                        .isOk();

                //pn-SsDocumenti check
                Map<String, List<String>> tags = documentEntityDynamoDbAsyncTable.getItem(builder -> builder.key(keyBuilder -> keyBuilder.partitionValue(PARTITION_ID))).getTags();
                assertThat(tags, aMapWithSize(1));
                assertThat(tags.get(IUN), hasSize(1));
                assertThat(tags.get(IUN), hasItem(tagValue));

                //pn-SsTags check
                var tagKeyValueEntity = tagsEntityDynamoDbAsyncTable.getItem(builder -> builder.key(keyBuilder -> keyBuilder.partitionValue(IUN + "~" + tagValue)));
                Assertions.assertNotNull(tagKeyValueEntity);
                assertThat(tagKeyValueEntity.getFileKeys(), hasSize(1));
                assertThat(tagKeyValueEntity.getFileKeys(), hasItem(PARTITION_ID));
            }

            /**
             * Inserimento di piu' valori su un tag multivalue e indicizzato
             * Risultato atteso: 200 OK
             */
            @Test
            void putTags_Set_Indexed_Multivalue_Ok() {
                String tagValue1 = "ABCDEF";
                String tagValue2 = "123456";
                Map<String, List<String>> setTags = Map.of(IUN, List.of(tagValue1, tagValue2));
                webTestClient.put().uri(PUT_TAGS_PATH)
                        .bodyValue(new TagsChanges().fileKey(PARTITION_ID).SET(setTags))
                        .exchange()
                        .expectStatus()
                        .isOk();

                //pn-SsDocumenti check
                Map<String, List<String>> tags = documentEntityDynamoDbAsyncTable.getItem(builder -> builder.key(keyBuilder -> keyBuilder.partitionValue(PARTITION_ID))).getTags();
                assertThat(tags, aMapWithSize(1));
                assertThat(tags.get(IUN), hasSize(2));
                assertThat(tags.get(IUN), hasItems(tagValue1, tagValue2));

                //pn-SsTags check
                var tagKeyValueEntity1 = tagsEntityDynamoDbAsyncTable.getItem(builder -> builder.key(keyBuilder -> keyBuilder.partitionValue(IUN + "~" + tagValue1)));
                var tagKeyValueEntity2 = tagsEntityDynamoDbAsyncTable.getItem(builder -> builder.key(keyBuilder -> keyBuilder.partitionValue(IUN + "~" + tagValue2)));
                assertThat(Arrays.asList(tagKeyValueEntity1, tagKeyValueEntity2), everyItem(notNullValue()));
                assertThat(Arrays.asList(tagKeyValueEntity1.getFileKeys(), tagKeyValueEntity2.getFileKeys()), everyItem(hasSize(1)));
                assertThat(Arrays.asList(tagKeyValueEntity1.getFileKeys(), tagKeyValueEntity2.getFileKeys()), everyItem(hasItem(PARTITION_ID)));
            }
        }

        /**
         * Inserimento di un singolo valore su un tag multivalue e non indicizzato
         * Risultato atteso: 200 OK
         */
        @Test
        void putTags_Set_NotIndexed_OneValue_Ok() {
            String tagValue = "ABCDEF";
            Map<String, List<String>> setTags = Map.of(TAG_MULTIVALUE_NOT_INDEXED, List.of(tagValue));
            webTestClient.put().uri(PUT_TAGS_PATH)
                    .bodyValue(new TagsChanges().fileKey(PARTITION_ID).SET(setTags))
                    .exchange()
                    .expectStatus()
                    .isOk();

            //pn-SsDocumenti check
            Map<String, List<String>> tags = documentEntityDynamoDbAsyncTable.getItem(builder -> builder.key(keyBuilder -> keyBuilder.partitionValue(PARTITION_ID))).getTags();
            assertThat(tags, aMapWithSize(1));
            assertThat(tags.get(TAG_MULTIVALUE_NOT_INDEXED), hasSize(1));
            assertThat(tags.get(TAG_MULTIVALUE_NOT_INDEXED), hasItem(tagValue));

            //pn-SsTags check
            var tagKeyValueEntity = tagsEntityDynamoDbAsyncTable.getItem(builder -> builder.key(keyBuilder -> keyBuilder.partitionValue(TAG_MULTIVALUE_NOT_INDEXED + "~" + tagValue)));
            Assertions.assertNull(tagKeyValueEntity);
        }

        /**
         * Inserimento di piu' valori su un tag multivalue e non indicizzato
         * Risultato atteso: 200 OK
         */
        @Test
        void putTags_Set_NotIndexed_Multivalue_Ok() {
            String tagValue1 = "ABCDEF";
            String tagValue2 = "123456";
            Map<String, List<String>> setTags = Map.of(TAG_MULTIVALUE_NOT_INDEXED, List.of(tagValue1, tagValue2));
            webTestClient.put().uri(PUT_TAGS_PATH)
                    .bodyValue(new TagsChanges().fileKey(PARTITION_ID).SET(setTags))
                    .exchange()
                    .expectStatus()
                    .isOk();

            //pn-SsDocumenti check
            Map<String, List<String>> tags = documentEntityDynamoDbAsyncTable.getItem(builder -> builder.key(keyBuilder -> keyBuilder.partitionValue(PARTITION_ID))).getTags();
            assertThat(tags, aMapWithSize(1));
            assertThat(tags.get(TAG_MULTIVALUE_NOT_INDEXED), hasSize(2));
            assertThat(tags.get(TAG_MULTIVALUE_NOT_INDEXED), hasItems(tagValue1, tagValue2));

            //pn-SsTags check
            var tagKeyValueEntity1 = tagsEntityDynamoDbAsyncTable.getItem(builder -> builder.key(keyBuilder -> keyBuilder.partitionValue(TAG_MULTIVALUE_NOT_INDEXED + "~" + tagValue1)));
            var tagKeyValueEntity2 = tagsEntityDynamoDbAsyncTable.getItem(builder -> builder.key(keyBuilder -> keyBuilder.partitionValue(TAG_MULTIVALUE_NOT_INDEXED + "~" + tagValue2)));
            assertThat(Arrays.asList(tagKeyValueEntity1, tagKeyValueEntity2), everyItem(nullValue()));
        }

        /**
         * Inserimento di piu' valori su un tag non multivalue
         * Risultato atteso: 400 BAD REQUEST
         */
        @Test
        void putTags_Set_Multivalue_Ko() {
            String tagValue1 = "OK";
            String tagValue2 = "KO";
            Map<String, List<String>> setTags = Map.of(CONSERVAZIONE, List.of(tagValue1, tagValue2));
            webTestClient.put().uri(PUT_TAGS_PATH)
                    .bodyValue(new TagsChanges().fileKey(PARTITION_ID).SET(setTags))
                    .exchange()
                    .expectStatus()
                    .isBadRequest();
        }

    }
}
