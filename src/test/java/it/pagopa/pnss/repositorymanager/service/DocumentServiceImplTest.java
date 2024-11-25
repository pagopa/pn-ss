package it.pagopa.pnss.repositorymanager.service;


import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.*;
import it.pagopa.pnss.common.model.dto.MacchinaStatiValidateStatoResponseDto;
import it.pagopa.pnss.common.rest.call.machinestate.CallMacchinaStati;
import it.pagopa.pnss.configurationproperties.RepositoryManagerDynamoTableName;
import it.pagopa.pnss.repositorymanager.entity.CurrentStatusEntity;
import it.pagopa.pnss.repositorymanager.entity.DocTypeEntity;
import it.pagopa.pnss.repositorymanager.entity.DocumentEntity;
import it.pagopa.pnss.repositorymanager.exception.IllegalDocumentStateException;
import it.pagopa.pnss.repositorymanager.exception.ItemAlreadyPresent;
import it.pagopa.pnss.repositorymanager.exception.RepositoryManagerException;
import it.pagopa.pnss.repositorymanager.service.impl.DocumentServiceImpl;
import it.pagopa.pnss.testutils.annotation.SpringBootTestWebEnv;
import lombok.CustomLog;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static it.pagopa.pnss.common.constant.Constant.ATTACHED;
import static it.pagopa.pnss.common.constant.Constant.AVAILABLE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTestWebEnv
@CustomLog
public class DocumentServiceImplTest {

    @Autowired
    private DocumentServiceImpl documentServiceImpl;
    @Autowired
    private static DynamoDbTable<DocTypeEntity> docTypeDynamoDbTable;
    @MockBean
    private static CallMacchinaStati callMacchinaStati;
    private static DynamoDbTable<DocumentEntity> documentDynamoDbTable;

    private static final String T1 = "T1";
    private static final String T2 = "T2";
    private static final String KEY = "documentKey";
    private static final String KEY2 = "documentKey2";
    private static final String KEY3 = "documentKey3";
    private static final String ALREADY_PRESENT = "alreadyPresent";


    @BeforeAll
    public static void insertDefaultDocuments(@Autowired DynamoDbEnhancedClient dynamoDbEnhancedClient,
                                             @Autowired RepositoryManagerDynamoTableName gestoreRepositoryDynamoDbTableName) {
        docTypeDynamoDbTable = dynamoDbEnhancedClient.table(gestoreRepositoryDynamoDbTableName.tipologieDocumentiName(), TableSchema.fromBean(DocTypeEntity.class));
        insertDocTypeEntities(generateDocTypeEntity(T1), generateDocTypeEntity(T2));
        documentDynamoDbTable = dynamoDbEnhancedClient.table(gestoreRepositoryDynamoDbTableName.documentiName(), TableSchema.fromBean(DocumentEntity.class));
        insertDocumentEntities(generateDocumentEntity(ALREADY_PRESENT));
    }

    @AfterAll
    public static void deleteDefaultDocuments(@Autowired DynamoDbEnhancedClient dynamoDbEnhancedClient,
                                             @Autowired RepositoryManagerDynamoTableName gestoreRepositoryDynamoDbTableName) {
        docTypeDynamoDbTable = dynamoDbEnhancedClient.table(gestoreRepositoryDynamoDbTableName.tipologieDocumentiName(), TableSchema.fromBean(DocTypeEntity.class));
        deleteDocTypeEntities(generateDocTypeEntity(T1), generateDocTypeEntity(T2));
        documentDynamoDbTable = dynamoDbEnhancedClient.table(gestoreRepositoryDynamoDbTableName.documentiName(), TableSchema.fromBean(DocumentEntity.class));
        deleteDocumentEntities(generateDocumentEntity(KEY), generateDocumentEntity(KEY2), generateDocumentEntity(KEY3), generateDocumentEntity(ALREADY_PRESENT));
    }


    @Test
    void insertDocumentTest() {
        DocumentInput documentInput = generateDocumentInput(KEY3);
        Mono<Document> result = Mono.fromCallable(() -> documentServiceImpl.insertDocument(documentInput)).flatMap(mono -> mono);
        StepVerifier.create(result).expectNextCount(1).verifyComplete();
    }

    @Test
    void insertDocumentItemAlreadyPresentTest() {
        DocumentInput documentInput = generateDocumentInput(ALREADY_PRESENT);
        Mono<Document> secondInsert = Mono.fromCallable(() -> documentServiceImpl.insertDocument(documentInput)).flatMap(mono -> mono);
        StepVerifier.create(secondInsert).verifyError(ItemAlreadyPresent.class);

    }

    @Test
    void insertDocumentNullTest() {
        Mono<Document> result = Mono.fromCallable(() -> documentServiceImpl.insertDocument(null)).flatMap(mono -> mono);
        StepVerifier.create(result).verifyError(RepositoryManagerException.class);
    }

    @Test
    void insertDocumentTestDocumentKeyNullTest() {
        DocumentInput documentInput = generateDocumentInput(null);
        Mono<Document> result = Mono.fromCallable(() -> documentServiceImpl.insertDocument(documentInput)).flatMap(mono -> mono);
        StepVerifier.create(result).verifyError(RepositoryManagerException.class);
    }

    @Test
    void updateDocumentStateTest() {
        DocumentChanges documentChanges = generateDocumentChanges();
        DocumentEntity documentEntity = generateDocumentEntity(KEY);
        Assertions.assertDoesNotThrow(() -> ReflectionTestUtils.invokeMethod(documentServiceImpl, "updateDocumentState", documentEntity, documentChanges));
    }

    @Test
    void handleDocumentStatusTransitionTest() {
        DocumentChanges documentChanges = generateDocumentChanges();
        DocumentEntity documentEntity = generateDocumentEntity(KEY);
        MacchinaStatiValidateStatoResponseDto macchinaStatiValidateStatoResponseDto = generateMacchinaStatiValidateStatoResponseDto(true);

        when(callMacchinaStati.statusValidation(any())).thenReturn(Mono.just(macchinaStatiValidateStatoResponseDto));

        Mono<DocumentEntity> result = Mono.fromCallable(() -> (Mono<DocumentEntity>) ReflectionTestUtils.invokeMethod(documentServiceImpl, "handleDocumentStatusTransition", documentEntity, documentChanges)).flatMap(mono -> mono);
        StepVerifier.create(result).expectNextCount(1).verifyComplete();
    }

    @Test
    void handleDocumentStatusTransitionBlankState() {
        DocumentChanges documentChanges = generateDocumentChanges();
        DocumentEntity documentEntity = generateDocumentEntity(KEY);
        documentChanges.setDocumentState("");

        Mono<DocumentEntity> result = Mono.fromCallable(() -> (Mono<DocumentEntity>) ReflectionTestUtils.invokeMethod(documentServiceImpl, "handleDocumentStatusTransition", documentEntity, documentChanges)).flatMap(mono -> mono);
        StepVerifier.create(result).expectNextCount(1).verifyComplete();
    }

    @Test
    void updateDocumentStateTestUpdated() {
        DocumentChanges documentChanges = generateDocumentChanges();
        documentChanges.setDocumentState("VALID_STATE");

        CurrentStatusEntity currentStatusEntity = generateCurrentStatusEntity();
        currentStatusEntity.setTechnicalState("VALID_STATE");

        DocTypeEntity docTypeEntity = generateDocTypeEntity(T1);
        docTypeEntity.setStatuses(Map.of("LOGICAL_STATE", currentStatusEntity));

        DocumentEntity documentEntity = new DocumentEntity();
        documentEntity.setDocumentType(docTypeEntity);

        Assertions.assertDoesNotThrow(() -> ReflectionTestUtils.invokeMethod(
                documentServiceImpl, "updateDocumentState", documentEntity, documentChanges));

        Assertions.assertEquals("LOGICAL_STATE", documentEntity.getDocumentLogicalState());
    }

    @Test
    void updateDocumentStateTestKo() {
        DocumentChanges documentChanges = generateDocumentChanges();
        DocumentEntity documentEntity = generateDocumentEntity(KEY);
        documentEntity.getDocumentType().setStatuses(null);
        Assertions.assertThrows(IllegalDocumentStateException.class, () -> ReflectionTestUtils.invokeMethod(documentServiceImpl, "updateDocumentState", documentEntity, documentChanges));
    }

    @Test
    void convertAndStoreDocumentEntity() {
        DocumentInput documentInput = generateDocumentInput(KEY);
        DocumentType documentType = generateDocumentType();
        Document document = generateDocument();

        Mono<DocumentEntity> result = ReflectionTestUtils.invokeMethod(documentServiceImpl, "convertAndStoreDocumentEntity", documentInput, documentType, document);

        assert result != null;
        StepVerifier.create(result).verifyComplete();
    }

    @Test
    void hasBeenPatchedTest() {
        DocumentChanges documentChanges = generateDocumentChanges();
        DocumentEntity documentEntity = generateDocumentEntity(KEY);
        Assertions.assertEquals(Boolean.TRUE, (Boolean) ReflectionTestUtils.invokeMethod(documentServiceImpl, "hasBeenPatched", documentEntity, documentChanges));
        documentChanges = generateDocumentChanges();
        documentChanges.setDocumentState("newDocumentState");
        Assertions.assertEquals(Boolean.FALSE, (Boolean) ReflectionTestUtils.invokeMethod(documentServiceImpl, "hasBeenPatched", documentEntity, documentChanges));
        documentChanges = generateDocumentChanges();
        documentChanges.setRetentionUntil("newRetentionUntil");
        Assertions.assertEquals(Boolean.FALSE, (Boolean) ReflectionTestUtils.invokeMethod(documentServiceImpl, "hasBeenPatched", documentEntity, documentChanges));
        documentChanges = generateDocumentChanges();
        documentChanges.setContentLenght(BigDecimal.TEN);
        Assertions.assertEquals(Boolean.FALSE, (Boolean) ReflectionTestUtils.invokeMethod(documentServiceImpl, "hasBeenPatched", documentEntity, documentChanges));
        documentChanges = generateDocumentChanges();
        documentChanges.setCheckSum("newCheckSum");
        Assertions.assertEquals(Boolean.FALSE, (Boolean) ReflectionTestUtils.invokeMethod(documentServiceImpl, "hasBeenPatched", documentEntity, documentChanges));
    }


    private DocumentInput generateDocumentInput(String key) {
        return new DocumentInput()
                .documentType(T1)
                .documentKey(key)
                .documentState(AVAILABLE)
                .checkSum("checkSum")
                .retentionUntil("retentionUntil")
                .contentLenght(BigDecimal.ONE)
                .contentType("contentType")
                .documentLogicalState("documentLogicalState")
                .clientShortCode("clientShortCode")
                .tags(Map.of("key", List.of("value")));
    }

    private DocumentType generateDocumentType() {
        return new DocumentType()
                .tipoDocumento(T1)
                .initialStatus("initialStatus")
                .informationClassification(DocumentType.InformationClassificationEnum.HC)
                .transformations(List.of(DocumentType.TransformationsEnum.SIGN_AND_TIMEMARK))
                .timeStamped(DocumentType.TimeStampedEnum.STANDARD)
                .checksum(DocumentType.ChecksumEnum.SHA256)
                .statuses(Map.of(AVAILABLE, new CurrentStatus()
                        .storage(AVAILABLE)
                        .allowedStatusTransitions(List.of(ATTACHED))));
    }

    private Document generateDocument() {
        return new Document()
                .documentType(generateDocumentType())
                .documentKey("documentKey")
                .documentState("documentState")
                .checkSum("checkSum")
                .retentionUntil("retentionUntil")
                .contentLenght(BigDecimal.ONE)
                .contentType("contentType")
                .documentLogicalState("documentLogicalState")
                .clientShortCode("clientShortCode")
                .tags(Map.of("key", List.of("value")));
    }

    private static DocTypeEntity generateDocTypeEntity(String tipoDocumento) {
        DocTypeEntity docTypeEntity = new DocTypeEntity();
        docTypeEntity.setTipoDocumento(tipoDocumento);
        docTypeEntity.setInitialStatus("PRELOADED");

        List<String> allowedStatusTransitions = new ArrayList<>();
        allowedStatusTransitions.add("ATTACHED");

        CurrentStatusEntity documentTypeConfigurationStatuses = new CurrentStatusEntity();
        documentTypeConfigurationStatuses.setStorage("AVAILABLE");
        documentTypeConfigurationStatuses.setAllowedStatusTransitions(allowedStatusTransitions);
        documentTypeConfigurationStatuses.setTechnicalState("AVAILABLE"); // Ensure technicalState is set
        docTypeEntity.setStatuses(Map.of("AVAILABLE", documentTypeConfigurationStatuses));

        docTypeEntity.setInformationClassification(DocumentType.InformationClassificationEnum.HC);
        docTypeEntity.setTransformations(List.of(DocumentType.TransformationsEnum.SIGN_AND_TIMEMARK));
        docTypeEntity.setTimeStamped(DocumentType.TimeStampedEnum.STANDARD);
        docTypeEntity.setChecksum(DocumentType.ChecksumEnum.SHA256.getValue());

        return docTypeEntity;
    }

    private static DocumentEntity generateDocumentEntity(String documentKey) {
        DocumentEntity documentEntity = new DocumentEntity();
        documentEntity.setDocumentKey(documentKey);
        documentEntity.setDocumentType(generateDocTypeEntity(T1));
        documentEntity.setDocumentState(AVAILABLE);
        documentEntity.setCheckSum("checkSum");
        documentEntity.setRetentionUntil("retentionUntil");
        documentEntity.setContentLenght(BigDecimal.ONE);
        documentEntity.setContentType("contentType");
        documentEntity.setDocumentLogicalState("documentLogicalState");
        documentEntity.setClientShortCode("clientShortCode");
        documentEntity.setTags(Map.of("key", List.of("value")));
        return documentEntity;
    }

    private static DocumentChanges generateDocumentChanges() {
        DocumentChanges documentChanges = new DocumentChanges();
        documentChanges.setDocumentState(AVAILABLE);
        documentChanges.setCheckSum("checkSum");
        documentChanges.setRetentionUntil("retentionUntil");
        documentChanges.setContentLenght(BigDecimal.ONE);
        return documentChanges;
    }

    private static MacchinaStatiValidateStatoResponseDto generateMacchinaStatiValidateStatoResponseDto(boolean allowed) {
        MacchinaStatiValidateStatoResponseDto macchinaStatiValidateStatoResponseDto = new MacchinaStatiValidateStatoResponseDto();
        macchinaStatiValidateStatoResponseDto.setMessage("message");
        macchinaStatiValidateStatoResponseDto.setNotificationMessage("notificationMessage");
        macchinaStatiValidateStatoResponseDto.setAllowed(allowed);
        return macchinaStatiValidateStatoResponseDto;
    }

    private static CurrentStatusEntity generateCurrentStatusEntity() {
        CurrentStatusEntity currentStatusEntity = new CurrentStatusEntity();
        currentStatusEntity.setStorage("storage");
        currentStatusEntity.setAllowedStatusTransitions(List.of("allowedStatusTransitions"));
        currentStatusEntity.setTechnicalState("technicalState");
        return currentStatusEntity;
    }

    private static void insertDocTypeEntities(DocTypeEntity... docTypeEntities) {
        Arrays.stream(docTypeEntities).forEach(docTypeEntity -> docTypeDynamoDbTable.putItem(docTypeEntity));
    }

    private static void deleteDocTypeEntities(DocTypeEntity... docTypeEntities) {
        Arrays.stream(docTypeEntities).forEach(docTypeEntity -> docTypeDynamoDbTable.deleteItem(docTypeEntity));
    }

    private static void deleteDocumentEntities(DocumentEntity... documentEntities) {
        Arrays.stream(documentEntities).forEach(documentEntity -> documentDynamoDbTable.deleteItem(documentEntity));
    }

    private static void insertDocumentEntities(DocumentEntity... documentEntities) {
        Arrays.stream(documentEntities).forEach(documentEntity -> documentDynamoDbTable.putItem(documentEntity));
    }
}
