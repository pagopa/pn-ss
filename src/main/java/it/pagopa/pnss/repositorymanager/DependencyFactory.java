package it.pagopa.pnss.repositorymanager;

import it.pagopa.pnss.repositorymanager.service.impl.UserConfigurationServiceImpl;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

/**
 * The module containing all dependencies required by the {@link UserConfigurationServiceImpl}.
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
