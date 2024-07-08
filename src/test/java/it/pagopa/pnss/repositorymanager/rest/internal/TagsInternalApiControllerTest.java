package it.pagopa.pnss.repositorymanager.rest.internal;

import it.pagopa.pnss.configurationproperties.RepositoryManagerDynamoTableName;
import it.pagopa.pnss.repositorymanager.entity.DocumentEntity;
import it.pagopa.pnss.repositorymanager.entity.TagsEntity;
import it.pagopa.pnss.testutils.annotation.SpringBootTestWebEnv;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.beans.factory.annotation.Autowired;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

@SpringBootTestWebEnv
class TagsInternalApiControllerTest {

    private static DynamoDbTable<TagsEntity> tagsEntityDynamoDbAsyncTable;
    private static DynamoDbTable<DocumentEntity> documentEntityDynamoDbAsyncTable;

    @BeforeAll
    public static void insertDefaultDocument(@Autowired DynamoDbEnhancedClient dynamoDbEnhancedClient,
                                             @Autowired RepositoryManagerDynamoTableName repositoryManagerDynamoTableName) {
        tagsEntityDynamoDbAsyncTable = dynamoDbEnhancedClient.table(repositoryManagerDynamoTableName.tagsName(), TableSchema.fromBean(TagsEntity.class));
        documentEntityDynamoDbAsyncTable = dynamoDbEnhancedClient.table(repositoryManagerDynamoTableName.documentiName(), TableSchema.fromBean(DocumentEntity.class));
    }

}
