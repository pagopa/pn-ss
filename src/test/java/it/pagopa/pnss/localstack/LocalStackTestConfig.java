package it.pagopa.pnss.localstack;

import static it.pagopa.pnss.common.QueueNameConstant.ALL_QUEUE_NAME_LIST;
import static it.pagopa.pnss.localstack.LocalStackUtils.DEFAULT_LOCAL_STACK_TAG;
import static java.util.Map.entry;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.DYNAMODB;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.S3;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.SNS;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.SQS;
import static software.amazon.awssdk.services.dynamodb.model.TableStatus.ACTIVE;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.utility.DockerImageName;

import it.pagopa.pnss.configurationproperties.BucketName;
import it.pagopa.pnss.configurationproperties.RepositoryManagerDynamoTableName;
import it.pagopa.pnss.repositoryManager.entity.DocTypeEntity;
import it.pagopa.pnss.repositoryManager.entity.DocumentEntity;
import it.pagopa.pnss.repositoryManager.entity.UserConfigurationEntity;
import it.pagopa.pnss.testutils.annotation.exception.DynamoDbInitTableCreationException;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.core.internal.waiters.ResponseOrException;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.DescribeTableResponse;
import software.amazon.awssdk.services.dynamodb.model.ResourceNotFoundException;
import software.amazon.awssdk.services.dynamodb.waiters.DynamoDbWaiter;
import software.amazon.awssdk.services.s3.model.LifecycleRuleFilter;
import software.amazon.awssdk.services.s3.model.Transition;
import software.amazon.awssdk.services.s3.model.TransitionStorageClass;

@TestConfiguration
@Slf4j
public class LocalStackTestConfig {

    @Autowired
    private DynamoDbClient dynamoDbClient;

    @Autowired
    private DynamoDbEnhancedClient dynamoDbEnhancedClient;

    @Autowired
    private DynamoDbWaiter dynamoDbWaiter;
    
    @Autowired
    private RepositoryManagerDynamoTableName repositoryManagerDynamoTableName;
    
    @Autowired
    private BucketName bucketName;


    static LocalStackContainer localStackContainer = new LocalStackContainer(DockerImageName.parse(DEFAULT_LOCAL_STACK_TAG)).withServices(
            SQS,
            DYNAMODB,
            S3);

    static {
        localStackContainer.start();
        
        System.setProperty("test.aws.region", localStackContainer.getRegion());

//      <-- Override spring-cloud-starter-aws-messaging endpoints for testing -->
        System.setProperty("cloud.aws.sqs.endpoint", String.valueOf(localStackContainer.getEndpointOverride(SQS)));

//      <-- Override AWS services endpoint variables for testing -->
        System.setProperty("test.aws.sqs.endpoint", String.valueOf(localStackContainer.getEndpointOverride(SQS)));
        System.setProperty("test.aws.dynamodb.endpoint", String.valueOf(localStackContainer.getEndpointOverride(DYNAMODB)));
        System.setProperty("test.aws.sns.endpoint", String.valueOf(localStackContainer.getEndpointOverride(SNS)));
        System.setProperty("test.aws.s3.endpoint", String.valueOf(localStackContainer.getEndpointOverride(S3)));
        System.setProperty("aws.config.access.key", localStackContainer.getAccessKey());
        System.setProperty("aws.config.secret.key", localStackContainer.getSecretKey());
        System.setProperty("aws.config.default.region", localStackContainer.getRegion());
        System.setProperty("aws.region",  localStackContainer.getRegion());
        System.setProperty("aws.access.key", localStackContainer.getAccessKey());
        System.setProperty("aws.secret.key", localStackContainer.getSecretKey());
        System.setProperty("PnSsStagingBucketName","PnSsStagingBucketName");

        System.setProperty("PnSsStagingBucketArn","PnSsStagingBucketArn");
//        System.setProperty("PnSsBucketName","PnSsBucketName");
        System.setProperty("PnSsBucketArn","PnSsBucketArn");



        try {

//          Create SQS queue
            for (String queueName : ALL_QUEUE_NAME_LIST) {
                localStackContainer.execInContainer("awslocal", "sqs", "create-queue", "--queue-name", queueName);
            }
            
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private void createTable(final String tableName, final Class<?> entityClass) {
        DynamoDbTable<?> dynamoDbTable = dynamoDbEnhancedClient.table(tableName, TableSchema.fromBean(entityClass));
        dynamoDbTable.createTable(builder -> builder.provisionedThroughput(b -> b.readCapacityUnits(5L).writeCapacityUnits(5L).build()));

        // La creazione delle tabelle su Dynamo è asincrona. Bisogna aspettare tramite il DynamoDbWaiter
        ResponseOrException<DescribeTableResponse> responseOrException = dynamoDbWaiter.waitUntilTableExists(builder -> builder.tableName(
                tableName).build()).matched();
        responseOrException.response().orElseThrow(() -> new DynamoDbInitTableCreationException(tableName));
    }
    
    //TODO aggiungere lifecycleRule per bucket relativo a PnSsBucketName
    private void addLifecycleConfigurationToHotBucket() {
    	//TODO metodo da completare
        LifecycleRuleFilter ruleFilter_PN_TEMPORARY_DOCUMENT = LifecycleRuleFilter.builder()
//                .prefix("glacierobjects/")
                .build();
        
        Transition transition = Transition.builder()
                .storageClass(TransitionStorageClass.STANDARD_IA)
                .days(0)
                .build();
    }
    
    @PostConstruct
    public void initLocalStack() {
    	
    	//TODO aggiungere lifecycleRule per bucket relativo a PnSsBucketName
    	List<String> allBucketNameList = List.of(bucketName.ssHotName(),bucketName.ssStageName());
        log.info("initLocalStack() : allBucketNameList  {}", allBucketNameList);
        for (String bucketName : allBucketNameList){
        	try {
        		localStackContainer.execInContainer("awslocal", "s3", "mb", "s3://" + bucketName);
            } catch (IOException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    	
        Map<String, Class<?>> tableNameWithEntityClass =
                Map.ofEntries(entry(repositoryManagerDynamoTableName.anagraficaClientName(), UserConfigurationEntity.class),
                			  entry(repositoryManagerDynamoTableName.tipologieDocumentiName(), DocTypeEntity.class),
                              entry(repositoryManagerDynamoTableName.documentiName(), DocumentEntity.class));
        
        tableNameWithEntityClass.forEach((tableName, entityClass) -> {
            log.info("<-- START initLocalStack -->");
            try {
                log.info("<-- START Dynamo db init-->");
                DescribeTableResponse describeTableResponse = dynamoDbClient.describeTable(builder -> builder.tableName(tableName));
                if (describeTableResponse.table().tableStatus() == ACTIVE) {
                    log.info("Table {} already created on local stack's dynamo db", tableName);
                }
            } catch (ResourceNotFoundException resourceNotFoundException) {
                log.info("Table {} not found on first dynamo init. Proceed to create", tableName);
                createTable(tableName, entityClass);
            }
        });
    }
}
