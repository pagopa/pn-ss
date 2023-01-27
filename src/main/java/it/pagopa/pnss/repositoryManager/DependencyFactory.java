package it.pagopa.pnss.repositoryManager;

import it.pagopa.pnss.repositoryManager.service.UserConfigurationService;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

/**
 * The module containing all dependencies required by the {@link UserConfigurationService}.
 */
public class DependencyFactory {

    private DependencyFactory() {}

    /**
     * @return an instance of DynamoDbClient
     */
    public static DynamoDbClient dynamoDbClient() {
        return DynamoDbClient.create();
    }

    public static DynamoDbEnhancedClient dynamoDbEnhancedClient(){
        DynamoDbClient ddb = DependencyFactory.dynamoDbClient();
        return DynamoDbEnhancedClient.builder().dynamoDbClient(ddb).build();
    }
}
