package it.pagopa.pnss.localstack;

//import static it.pagopa.pnss.constant.QueueNameConstant.*;
import static it.pagopa.pnss.localstack.LocalStackUtils.DEFAULT_LOCAL_STACK_TAG;
import static it.pagopa.pnss.repositoryManager.constant.DynamoTableNameConstant.ANAGRAFICA_CLIENT_TABLE_NAME;
import static it.pagopa.pnss.repositoryManager.constant.DynamoTableNameConstant.DOCUMENT_TABLE_NAME;
import static it.pagopa.pnss.repositoryManager.constant.DynamoTableNameConstant.DOC_TYPES_TABLE_NAME;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.DYNAMODB;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.utility.DockerImageName;

import it.pagopa.pnss.repositoryManager.model.DocTypesEntity;
import it.pagopa.pnss.repositoryManager.model.DocumentEntity;
import it.pagopa.pnss.repositoryManager.model.UserConfigurationEntity;
import software.amazon.awssdk.core.internal.waiters.ResponseOrException;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.services.dynamodb.model.DescribeTableResponse;
import software.amazon.awssdk.services.dynamodb.waiters.DynamoDbWaiter;

@TestConfiguration
public class LocalStackTestConfig {

    @Autowired
    private DynamoDbEnhancedClient enhancedClient;

    @Autowired
    private DynamoDbWaiter dynamoDbWaiter;

    static LocalStackContainer localStackContainer = new LocalStackContainer(DockerImageName.parse(DEFAULT_LOCAL_STACK_TAG)).withServices(
//            SQS,
            DYNAMODB);

    static {
        localStackContainer.start();

//      Override aws config
        System.setProperty("aws.config.access.key", localStackContainer.getAccessKey());
        System.setProperty("aws.config.secret.key", localStackContainer.getSecretKey());
        System.setProperty("aws.config.default.region", localStackContainer.getRegion());

////      SQS Override Endpoint
//        System.setProperty("aws.sqs.test.endpoint", String.valueOf(localStackContainer.getEndpointOverride(SQS)));

//      DynamoDb Override Endpoint
        //System.setProperty("aws.dynamodb.test.endpoint", String.valueOf(localStackContainer.getEndpointOverride(DYNAMODB)));
        System.setProperty("test.aws.dynamodb.endpoint", String.valueOf(localStackContainer.getEndpointOverride(DYNAMODB)));
        
//        try {
//
////          Create SQS queue
//            localStackContainer.execInContainer(createQueueCliCommand(NOTIFICATION_TRACKER_QUEUE_NAME));
//            localStackContainer.execInContainer(createQueueCliCommand(SMS_QUEUE_NAME));
//            localStackContainer.execInContainer(createQueueCliCommand(SMS_ERROR_QUEUE_NAME));
//
//            // TODO: Create DynamoDb schemas
//        } catch (IOException | InterruptedException e) {
//            throw new RuntimeException(e);
//        }
    }
    
    @PostConstruct
    public void createTableAnagraficaClient() {
        DynamoDbTable<UserConfigurationEntity> userConfigurationTable = enhancedClient.table(ANAGRAFICA_CLIENT_TABLE_NAME,
                                                                                             TableSchema.fromBean(UserConfigurationEntity.class));
        userConfigurationTable.createTable(builder -> builder.provisionedThroughput(b -> b.readCapacityUnits(5L)
                                                                                          .writeCapacityUnits(5L)
                                                                                          .build()));
   
        ResponseOrException<DescribeTableResponse> responseUserConfiguration = dynamoDbWaiter.waitUntilTableExists(builder -> builder.tableName(ANAGRAFICA_CLIENT_TABLE_NAME).build()).matched();
        DescribeTableResponse tableDescriptionUserConfiguration = responseUserConfiguration.response()
                                                         .orElseThrow(() -> new RuntimeException("User Configuration table was not created."));
        // The actual error can be inspected in response.exception()
        
        DynamoDbTable<DocTypesEntity> docTypesTable = enhancedClient.table(DOC_TYPES_TABLE_NAME,
		                												   TableSchema.fromBean(DocTypesEntity.class));
		docTypesTable.createTable(builder -> builder.provisionedThroughput(b -> b.readCapacityUnits(5L)
															                      .writeCapacityUnits(5L)
															                      .build()));
		ResponseOrException<DescribeTableResponse> responseDocTypes = dynamoDbWaiter.waitUntilTableExists(builder -> builder.tableName(DOC_TYPES_TABLE_NAME).build()).matched();
		DescribeTableResponse tableDescriptionDocTypes= responseDocTypes.response()
															.orElseThrow(() -> new RuntimeException("Doc Types table was not created."));
		// The actual error can be inspected in response.exception()
		
        DynamoDbTable<DocumentEntity> documentTable = enhancedClient.table(DOCUMENT_TABLE_NAME,
		                													TableSchema.fromBean(DocumentEntity.class));
		documentTable.createTable(builder -> builder.provisionedThroughput(b -> b.readCapacityUnits(5L)
															                      .writeCapacityUnits(5L)
															                      .build()));
		ResponseOrException<DescribeTableResponse> responseDocument = dynamoDbWaiter.waitUntilTableExists(builder -> builder.tableName(DOCUMENT_TABLE_NAME).build()).matched();
		DescribeTableResponse tableDescriptionDocument = responseDocument.response()
		.orElseThrow(() -> new RuntimeException("Document table was not created."));
		// The actual error can be inspected in response.exception()

    }
    
}
