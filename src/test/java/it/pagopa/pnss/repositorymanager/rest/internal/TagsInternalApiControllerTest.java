package it.pagopa.pnss.repositorymanager.rest.internal;

import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.TagsResponse;
import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.TagsChanges;
import it.pagopa.pnss.configurationproperties.RepositoryManagerDynamoTableName;
import it.pagopa.pnss.repositorymanager.entity.DocumentEntity;
import it.pagopa.pnss.repositorymanager.entity.TagsRelationsEntity;
import it.pagopa.pnss.repositorymanager.service.TagsService;
import it.pagopa.pnss.testutils.annotation.SpringBootTestWebEnv;
import lombok.CustomLog;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.web.reactive.server.WebTestClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

import java.util.ArrayList;
import java.util.List;

import static org.springframework.http.MediaType.APPLICATION_JSON;

import java.util.Arrays;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertNull;


@SpringBootTestWebEnv
@AutoConfigureWebTestClient(timeout = "50000")
@CustomLog
class TagsInternalApiControllerTest {
    @Value("${test.aws.s3.endpoint:#{null}}")
    String testAwsS3Endpoint;
    @Autowired
    private WebTestClient webTestClient;
    private static DynamoDbTable<TagsRelationsEntity> tagsEntityDynamoDbAsyncTable;
    private static DynamoDbTable<DocumentEntity> documentEntityDynamoDbAsyncTable;
    private static final String PUT_TAGS_PATH = "/safestorage/internal/v1/documents/{documentKey}/tags";


    @BeforeAll
    public static void insertDefaultDocument(@Autowired DynamoDbEnhancedClient dynamoDbEnhancedClient,
                                             @Autowired RepositoryManagerDynamoTableName repositoryManagerDynamoTableName) {
        tagsEntityDynamoDbAsyncTable = dynamoDbEnhancedClient.table(repositoryManagerDynamoTableName.tagsName(), TableSchema.fromBean(TagsRelationsEntity.class));
        documentEntityDynamoDbAsyncTable = dynamoDbEnhancedClient.table(repositoryManagerDynamoTableName.documentiName(), TableSchema.fromBean(DocumentEntity.class));
        GetTagsTest.insertTagEntity();
    }


    @Nested
    class GetTagsTest {
        private static final String BASE_PATH = "/safestorage/internal/v1";
        private static final String BASE_PATH_WITH_PARAM = String.format("%s/tags/{tagKeyValue}", BASE_PATH);
        private static final String TAG_KEY_DEFAULT = "tagKeyTest";
        private static final String TAG_KEY_DEFAULT_NOT_EXIST = "tagKeyNotExist";

        private static void insertTagEntity() {
            log.info("execute insertTagsEntity()");

            TagsRelationsEntity tagsRelationsEntity = new TagsRelationsEntity();
            tagsRelationsEntity.setTagKeyValue(GetTagsTest.TAG_KEY_DEFAULT);

            List<String> fileKeys = new ArrayList<>();
            fileKeys.add("FILE_1");
            fileKeys.add("FILE_2");

            tagsRelationsEntity.setFileKeys(fileKeys);
            log.info("execute insertTagsEntity() : tagsEntity : {}", tagsRelationsEntity);
            tagsEntityDynamoDbAsyncTable.putItem(builder -> builder.item(tagsRelationsEntity));
        }


        /**
         * GET sulla tabella pn-SsTags di un tag con associazione fileKey
         * Risulato atteso: 200 OK
         */
        @Test
        void getTags_success() {

            webTestClient.get()
                    .uri(uriBuilder -> uriBuilder.path(BASE_PATH_WITH_PARAM).build(TAG_KEY_DEFAULT))
                    .accept(APPLICATION_JSON)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody(TagsResponse.class);

            System.out.println("\n Test getTags_success passed \n");
        }


        /**
         * GET sulla tabella pn-SsTags di un tag con partitionKey non esistente.
         * Risulato atteso 404 NOT FOUND
         */
        @Test
        void getTags_ko() {

            webTestClient.get()
                    .uri(uriBuilder -> uriBuilder.path(BASE_PATH_WITH_PARAM).build(TAG_KEY_DEFAULT_NOT_EXIST))
                    .accept(APPLICATION_JSON)
                    .exchange()
                    .expectStatus().isNotFound()
                    .expectBody(TagsResponse.class);

            System.out.println("\n Test getTags_ko passed \n");
        }
    }
    @Autowired
    private TagsService tagsService;

    @Nested
    class PutTagsTest {
        // Tag multivalue e indicizzato
        private static final String IUN = "IUN";
        // Tag singlevalue e non indicizzato
        private static final String CONSERVAZIONE = "Conservazione";
        // Tag multivalue e non indicizzato
        private static final String TAG_MULTIVALUE_NOT_INDEXED = "TAG_MULTIVALUE_NOT_INDEXED";
        // Tag singlevalue e indicizzato
        private static final String TAG_SINGLEVALUE_INDEXED = "TAG_SINGLEVALUE_INDEXED";

        @AfterEach
        void afterEach() {
            // Tables clean-up
            for (var page : documentEntityDynamoDbAsyncTable.scan()) {
                for (var item : page.items()) {
                    documentEntityDynamoDbAsyncTable.deleteItem(item);
                }
            }
            for (var page : tagsEntityDynamoDbAsyncTable.scan()) {
                for (var item : page.items()) {
                    tagsEntityDynamoDbAsyncTable.deleteItem(item);
                }
            }
        }

        // Serie di test su un documento senza tag sulla pn-SsDocumenti o associazioni sulla pn-SsTags (documento pulito)
        @Nested
        class UpdateDocumentWithNoTags {
            private static final String PARTITION_ID = "UpdateDocumentWithNoTagsTest";

            @BeforeEach
            void beforeEach() {
                DocumentEntity documentEntity = new DocumentEntity();
                documentEntity.setDocumentKey(PARTITION_ID);
                documentEntityDynamoDbAsyncTable.putItem(documentEntityBuilder -> documentEntityBuilder.item(documentEntity));
            }

            /**
             * Inserimento di un singolo valore su un tag multivalue e indicizzato.
             * Risultato atteso: 200 OK
             */
            @Test
            void putTags_Set_Indexed_OneValue_Ok() {
                String tagValue = "ABCDEF";
                Map<String, List<String>> setTags = Map.of(IUN, List.of(tagValue));
                webTestClient.put().uri(uriBuilder -> uriBuilder.path(PUT_TAGS_PATH).build(PARTITION_ID))
                        .bodyValue(new TagsChanges().SET(setTags))
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
                webTestClient.put().uri(uriBuilder -> uriBuilder.path(PUT_TAGS_PATH).build(PARTITION_ID))
                        .bodyValue(new TagsChanges().SET(setTags))
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

            /**
             * Inserimento di un singolo valore su un tag multivalue e non indicizzato
             * Risultato atteso: 200 OK
             */
            @Test
            void putTags_Set_NotIndexed_OneValue_Ok() {
                String tagValue = "ABCDEF";
                Map<String, List<String>> setTags = Map.of(TAG_MULTIVALUE_NOT_INDEXED, List.of(tagValue));
                webTestClient.put().uri(uriBuilder -> uriBuilder.path(PUT_TAGS_PATH).build(PARTITION_ID))
                        .bodyValue(new TagsChanges().SET(setTags))
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
                assertNull(tagKeyValueEntity);
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
                webTestClient.put().uri(uriBuilder -> uriBuilder.path(PUT_TAGS_PATH).build(PARTITION_ID))
                        .bodyValue(new TagsChanges().SET(setTags))
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
             * Aggiornamento dei tag su un documento non esistente.
             * Risultato atteso: 400 BAD_REQUEST
             */
            @Test
            void putTags_NonExistingDocument_Ko() {
                String tagValue = "ABCDEF";

                // Update
                Map<String, List<String>> setTags = Map.of(IUN, List.of(tagValue));
                webTestClient.put().uri(uriBuilder -> uriBuilder.path(PUT_TAGS_PATH).build("NonExistingDocumentKey"))
                        .bodyValue(new TagsChanges().SET(setTags))
                        .exchange()
                        .expectStatus()
                        .isNotFound();
            }

            /**
             * Aggiornamento di un tag non valido.
             * Risultato atteso: 400 BAD_REQUEST
             */
            @Test
            void putTags_MissingTag_Ko() {
                String tagValue = "ABCDEF";

                // Update
                Map<String, List<String>> setTags = Map.of("NonExistingTag", List.of(tagValue));
                webTestClient.put().uri(uriBuilder -> uriBuilder.path(PUT_TAGS_PATH).build(PARTITION_ID))
                        .bodyValue(new TagsChanges().SET(setTags))
                        .exchange()
                        .expectStatus()
                        .isBadRequest();
            }

        }

        @Nested
        class UpdateDocumentWithTags {
            private static final String PARTITION_ID = "UpdateDocumentWithTagsTest";

            @BeforeEach
            void beforeEach() {
                DocumentEntity documentEntity = new DocumentEntity();
                documentEntity.setDocumentKey(PARTITION_ID);
                documentEntityDynamoDbAsyncTable.putItem(documentEntityBuilder -> documentEntityBuilder.item(documentEntity));
            }

            /**
             * Aggiornamento di un tag indicizzato e singlevalue, modalità di aggiornamento replace.
             * Risultato atteso: 200 OK
             */
            @Test
            void putTags_Set_Indexed_Singlevalue_Ok() {
                String initialTagValue = "initialTagValue";
                String newTagValue = "newTagValue";
                // Setup
                tagsService.putTags(PARTITION_ID, new TagsChanges().SET(Map.of(TAG_SINGLEVALUE_INDEXED, List.of(initialTagValue)))).block();

                // Update
                Map<String, List<String>> setTags = Map.of(TAG_SINGLEVALUE_INDEXED, List.of(newTagValue));
                webTestClient.put().uri(uriBuilder -> uriBuilder.path(PUT_TAGS_PATH).build(PARTITION_ID))
                        .bodyValue(new TagsChanges().SET(setTags))
                        .exchange()
                        .expectStatus()
                        .isOk();

                //pn-SsDocumenti check
                Map<String, List<String>> tags = documentEntityDynamoDbAsyncTable.getItem(builder -> builder.key(keyBuilder -> keyBuilder.partitionValue(PARTITION_ID))).getTags();
                assertThat(tags, aMapWithSize(1));
                assertThat(tags.get(TAG_SINGLEVALUE_INDEXED), hasSize(1));
                assertThat(tags.get(TAG_SINGLEVALUE_INDEXED), hasItems(newTagValue));

                //pn-SsTags check
                var tagKeyValueEntity1 = tagsEntityDynamoDbAsyncTable.getItem(builder -> builder.key(keyBuilder -> keyBuilder.partitionValue(TAG_SINGLEVALUE_INDEXED + "~" + newTagValue)));
                assertThat(tagKeyValueEntity1.getFileKeys(), hasSize(1));
                assertThat(tagKeyValueEntity1.getFileKeys(), hasItem(PARTITION_ID));
            }

            /**
             * Aggiornamento di un tag non indicizzato e singlevalue, modalità di aggiornamento replace.
             * Nessuna scrittura su pn-SsTags
             * Risultato atteso: 200 OK
             */
            @Test
            void putTags_Set_NotIndexed_Singlevalue_Ok() {
                String initialTagValue = "initialTagValue";
                String newTagValue = "newTagValue";
                // Setup
                tagsService.putTags(PARTITION_ID, new TagsChanges().SET(Map.of(CONSERVAZIONE, List.of(initialTagValue)))).block();

                // Update
                Map<String, List<String>> setTags = Map.of(CONSERVAZIONE, List.of(newTagValue));
                webTestClient.put().uri(uriBuilder -> uriBuilder.path(PUT_TAGS_PATH).build(PARTITION_ID))
                        .bodyValue(new TagsChanges().SET(setTags))
                        .exchange()
                        .expectStatus()
                        .isOk();

                //pn-SsDocumenti check
                Map<String, List<String>> tags = documentEntityDynamoDbAsyncTable.getItem(builder -> builder.key(keyBuilder -> keyBuilder.partitionValue(PARTITION_ID))).getTags();
                assertThat(tags, aMapWithSize(1));
                assertThat(tags.get(CONSERVAZIONE), hasSize(1));
                assertThat(tags.get(CONSERVAZIONE), hasItems(newTagValue));

                //pn-SsTags check
                var tagKeyValueEntity = tagsEntityDynamoDbAsyncTable.getItem(builder -> builder.key(keyBuilder -> keyBuilder.partitionValue(TAG_SINGLEVALUE_INDEXED + "~" + newTagValue)));
                assertNull(tagKeyValueEntity);
            }

            /**
             * Aggiornamento di un tag indicizzato e multivalue, modalità di aggiornamento merge.
             * Risultato atteso: 200 OK
             */
            @Test
            void putTags_Set_Indexed_Multivalue_Ok() {
                String tagValue1 = "ABCDEF";
                String tagValue2 = "123456";
                // Setup
                tagsService.putTags(PARTITION_ID, new TagsChanges().SET(Map.of(IUN, List.of(tagValue1)))).block();

                // Update
                Map<String, List<String>> setTags = Map.of(IUN, List.of(tagValue1, tagValue2));
                webTestClient.put().uri(uriBuilder -> uriBuilder.path(PUT_TAGS_PATH).build(PARTITION_ID))
                        .bodyValue(new TagsChanges().SET(setTags))
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

            /**
             * Aggiornamento di un tag non indicizzato e multivalue, modalità di aggiornamento merge.
             * Nessuna scrittura su pn-SsTags
             * Risultato atteso: 200 OK
             */
            @Test
            void putTags_Set_NotIndexed_Multivalue_Ok() {
                String tagValue1 = "ABCDEF";
                String tagValue2 = "123456";
                // Setup
                tagsService.putTags(PARTITION_ID, new TagsChanges().SET(Map.of(TAG_MULTIVALUE_NOT_INDEXED, List.of(tagValue1)))).block();

                // Update
                Map<String, List<String>> setTags = Map.of(TAG_MULTIVALUE_NOT_INDEXED, List.of(tagValue1, tagValue2));
                webTestClient.put().uri(uriBuilder -> uriBuilder.path(PUT_TAGS_PATH).build(PARTITION_ID))
                        .bodyValue(new TagsChanges().SET(setTags))
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
        }

        @Nested
        class DeleteTags {
            private static final String PARTITION_ID = "DocumentDeleteTagsTest";

            @BeforeEach
            void beforeEach() {
                DocumentEntity documentEntity = new DocumentEntity();
                documentEntity.setDocumentKey(PARTITION_ID);
                documentEntityDynamoDbAsyncTable.putItem(documentEntityBuilder -> documentEntityBuilder.item(documentEntity));
            }

            /**
             * Rimozione di un tag singlevalue e indicizzato.
             * Eliminazione associazione su pn-SsTags.
             * Mappa dei tag in pn-SsDocumenti vuota.
             * Risultato atteso: 200 OK
             */
            @Test
            void putTags_Delete_Singlevalue_Indexed_Ok() {
                String tagValue = "OK";
                // Setup
                tagsService.putTags(PARTITION_ID, new TagsChanges().SET(Map.of(CONSERVAZIONE, List.of(tagValue)))).block();

                // Delete
                Map<String, List<String>> deleteTags = Map.of(CONSERVAZIONE, List.of(tagValue));
                webTestClient.put().uri(uriBuilder -> uriBuilder.path(PUT_TAGS_PATH).build(PARTITION_ID))
                        .bodyValue(new TagsChanges().DELETE(deleteTags))
                        .exchange()
                        .expectStatus()
                        .isOk();

                //pn-SsDocumenti check
                Map<String, List<String>> tags = documentEntityDynamoDbAsyncTable.getItem(builder -> builder.key(keyBuilder -> keyBuilder.partitionValue(PARTITION_ID))).getTags();
                assertThat(tags, aMapWithSize(0));

                //pn-SsTags check
                var tagKeyValueEntity = tagsEntityDynamoDbAsyncTable.getItem(builder -> builder.key(keyBuilder -> keyBuilder.partitionValue(CONSERVAZIONE + "~" + tagValue)));
                assertNull(tagKeyValueEntity);
            }

            /**
             * Rimozione di un valore per un tag multivalue e indicizzato.
             * Eliminazione di una associazione su pn-SsTags. Mantenimento dell'altra.
             * Rimozione di un valore dalla mappa dei tag in pn-SsDocumenti.
             * Risultato atteso: 200 OK
             */
            @Test
            void putTags_Delete_Multivalue_Indexed_Ok() {
                String tagValue1 = "ABCDEF";
                String tagValue2 = "123456";
                // Setup
                tagsService.putTags(PARTITION_ID, new TagsChanges().SET(Map.of(IUN, List.of(tagValue1, tagValue2)))).block();

                // Delete
                Map<String, List<String>> setTags = Map.of(IUN, List.of(tagValue2));
                webTestClient.put().uri(uriBuilder -> uriBuilder.path(PUT_TAGS_PATH).build(PARTITION_ID))
                        .bodyValue(new TagsChanges().DELETE(setTags))
                        .exchange()
                        .expectStatus()
                        .isOk();

                //pn-SsDocumenti check
                Map<String, List<String>> tags = documentEntityDynamoDbAsyncTable.getItem(builder -> builder.key(keyBuilder -> keyBuilder.partitionValue(PARTITION_ID))).getTags();
                assertThat(tags, aMapWithSize(1));
                assertThat(tags.get(IUN), hasSize(1));
                assertThat(tags.get(IUN), hasItems(tagValue1));

                //pn-SsTags check
                var tagKeyValueEntity1 = tagsEntityDynamoDbAsyncTable.getItem(builder -> builder.key(keyBuilder -> keyBuilder.partitionValue(IUN + "~" + tagValue1)));
                var tagKeyValueEntity2 = tagsEntityDynamoDbAsyncTable.getItem(builder -> builder.key(keyBuilder -> keyBuilder.partitionValue(IUN + "~" + tagValue2)));
                assertThat(tagKeyValueEntity1, notNullValue());
                assertThat(tagKeyValueEntity1.getFileKeys(), hasItem(PARTITION_ID));
                assertNull(tagKeyValueEntity2);
            }

        }

        @Nested
        class LimitsTest {
            private static final String PARTITION_ID = "TagsLimitsTest";

            @BeforeEach
            void beforeEach() {
                DocumentEntity documentEntity = new DocumentEntity();
                documentEntity.setDocumentKey(PARTITION_ID);
                documentEntityDynamoDbAsyncTable.putItem(documentEntityBuilder -> documentEntityBuilder.item(documentEntity));
            }

            /**
             * Test in cui viene superato il numero massimo di valori associabili ad un tag.
             * Il default va impostato a 5.
             * Risultato atteso: 400 BAD REQUEST
             */
            @Test
            void putTags_MaxValuesPerTagDocument_Ko() {
                String tagValue1 = "ABCDEF";
                String tagValue2 = "123456";
                String tagValue3 = "GHIJKL";
                String tagValue4 = "78910";
                String tagValue5 = "MNOPQR";
                String tagValue6 = "STUVWX";
                // Setup
                tagsService.putTags(PARTITION_ID, new TagsChanges().SET(Map.of(IUN, List.of(tagValue1, tagValue2)))).block();

                // Update
                Map<String, List<String>> setTags = Map.of(IUN, List.of(tagValue3, tagValue4, tagValue5, tagValue6));
                webTestClient.put().uri(uriBuilder -> uriBuilder.path(PUT_TAGS_PATH).build(PARTITION_ID))
                        .bodyValue(new TagsChanges().SET(setTags))
                        .exchange()
                        .expectStatus()
                        .isBadRequest();

            }

            /**
             * Test in cui viene superato il numero massimo di tag associabili ad una entry della pn-SsDocumenti
             * Il default va impostato a 2.
             * Risultato atteso: 400 BAD REQUEST
             */
            @Test
            void putTags_MaxTagsPerDocument_Ko() {

                // Setup
                tagsService.putTags(PARTITION_ID, new TagsChanges().SET(Map.of(IUN, List.of("tagValue1")))).block();

                // Update
                Map<String, List<String>> setTags = Map.of(CONSERVAZIONE, List.of("tagValue2"), TAG_MULTIVALUE_NOT_INDEXED, List.of("tagValue3"));
                webTestClient.put().uri(uriBuilder -> uriBuilder.path(PUT_TAGS_PATH).build(PARTITION_ID))
                        .bodyValue(new TagsChanges().SET(setTags))
                        .exchange()
                        .expectStatus()
                        .isBadRequest();

            }


            /**
             * Test in cui viene superato il numero massimo di fileKey associabili ad una coppia chiave-valore all'interno della pn-SsTags.
             * Il default va impostato a 5.
             * Risultato atteso: 400 BAD REQUEST
             */
            @Test
            void putTags_MaxFileKeys_Ko() {
                String tagValue = "ABCDEF";
                String LAST_PARTITION_ID = PARTITION_ID + "5";
                DocumentEntity lastDocumentEntity = new DocumentEntity();
                lastDocumentEntity.setDocumentKey(LAST_PARTITION_ID);
                documentEntityDynamoDbAsyncTable.putItem(documentEntityBuilder -> documentEntityBuilder.item(lastDocumentEntity));

                // Setup
                for (int i = 0; i < 5; i++) {
                    DocumentEntity documentEntity = new DocumentEntity();
                    String documentKey = PARTITION_ID + i;
                    documentEntity.setDocumentKey(documentKey);
                    documentEntityDynamoDbAsyncTable.putItem(documentEntityBuilder -> documentEntityBuilder.item(documentEntity));
                    tagsService.putTags(documentKey, new TagsChanges().SET(Map.of(IUN, List.of(tagValue)))).block();
                }
                // Update
                Map<String, List<String>> setTags = Map.of(IUN, List.of(tagValue));
                webTestClient.put().uri(uriBuilder -> uriBuilder.path(PUT_TAGS_PATH).build(LAST_PARTITION_ID))
                        .bodyValue(new TagsChanges().SET(setTags))
                        .exchange()
                        .expectStatus()
                        .isBadRequest();
            }

        }

    }
}
