package it.pagopa.pnss.repositorymanager.service;

import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.CurrentStatus;
import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.DocumentType;
import it.pagopa.pnss.configurationproperties.RepositoryManagerDynamoTableName;
import it.pagopa.pnss.repositorymanager.entity.CurrentStatusEntity;
import it.pagopa.pnss.repositorymanager.entity.DocTypeEntity;
import it.pagopa.pnss.repositorymanager.service.impl.DocumentsConfigsServiceImpl;
import it.pagopa.pnss.repositorymanager.service.impl.StorageConfigurationsServiceImpl;
import it.pagopa.pnss.testutils.annotation.SpringBootTestWebEnv;
import it.pagopa.pnss.transformation.service.impl.S3ServiceImpl;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.services.s3.model.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTestWebEnv
public class DocumentsConfigsServiceImplTest {

    @Autowired
    StorageConfigurationsServiceImpl storageConfigurationsService;
    @Autowired
    DocumentsConfigsServiceImpl documentsConfigsService;
    @MockBean
    S3ServiceImpl s3Service;
    private static DynamoDbTable<DocTypeEntity> docTypeDynamoDbTable;

    @BeforeAll
    public static void insertDefaultDocument(@Autowired DynamoDbEnhancedClient dynamoDbEnhancedClient,
                                             @Autowired RepositoryManagerDynamoTableName gestoreRepositoryDynamoDbTableName) {
        docTypeDynamoDbTable = dynamoDbEnhancedClient.table(gestoreRepositoryDynamoDbTableName.tipologieDocumentiName(), TableSchema.fromBean(DocTypeEntity.class));
        insertDocTypeEntities(createDocTypeEntity("T1"), createDocTypeEntity("T2"));
    }

    @Test
    void getDocumentsConfigsOk(){
        List<Transition> transitions = new ArrayList<>();
        transitions.add(Transition.builder().days(1).build());
        transitions.add(Transition.builder().days(2).build());

        LifecycleRule lifecycleRule = LifecycleRule.builder()
                .id("01")
                .filter(LifecycleRuleFilter.builder()
                        .and(LifecycleRuleAndOperator.builder()
                                .prefix("prefix")
                                .tags(Tag.builder()
                                        .key("storageType")
                                        .value("value")
                                        .build())
                                .build())
                        .build())
                .expiration(LifecycleExpiration.builder().days(500).build())
                .transitions(transitions)
                .build();

        GetBucketLifecycleConfigurationResponse getBucketResponse = GetBucketLifecycleConfigurationResponse.builder().rules(List.of(lifecycleRule)).build();
        when(s3Service.getBucketLifecycleConfiguration(anyString())).thenReturn(Mono.just(getBucketResponse));

        var testMono = documentsConfigsService.getDocumentsConfigs();

        StepVerifier.create(testMono).expectNextCount(1).verifyComplete();
    }

    private static void insertDocTypeEntities(DocTypeEntity... docTypeEntities) {
        Arrays.stream(docTypeEntities).forEach(docTypeEntity -> docTypeDynamoDbTable.putItem(docTypeEntity));
    }

    private static DocTypeEntity createDocTypeEntity(String tipoDocumento) {
        DocTypeEntity docTypeEntity = new DocTypeEntity();
        docTypeEntity.setTipoDocumento(tipoDocumento);
        docTypeEntity.setInitialStatus("PRELOADED");

        List<String> allowedStatusTransitions = new ArrayList<>();
        allowedStatusTransitions.add("ATTACHED");

        CurrentStatusEntity documentTypeConfigurationStatuses = new CurrentStatusEntity();
        documentTypeConfigurationStatuses.setStorage("AVAILABLE");
        documentTypeConfigurationStatuses.setAllowedStatusTransitions(allowedStatusTransitions);
        docTypeEntity.setStatuses(Map.of("AVAILABLE", documentTypeConfigurationStatuses));

        docTypeEntity.setInformationClassification(DocumentType.InformationClassificationEnum.HC);
        docTypeEntity.setTransformations(List.of(DocumentType.TransformationsEnum.SIGN_AND_TIMEMARK));
        docTypeEntity.setTimeStamped(DocumentType.TimeStampedEnum.STANDARD);
        docTypeEntity.setChecksum(DocumentType.ChecksumEnum.SHA256.getValue());

        return docTypeEntity;
    }

}
