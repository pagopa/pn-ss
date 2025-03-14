package it.pagopa.pnss.localstack;

import static it.pagopa.pnss.localstack.LocalStackUtils.DEFAULT_LOCAL_STACK_TAG;
import static it.pagopa.pnss.utils.QueueNameConstant.ALL_QUEUE_NAME_LIST;
import static java.util.Map.entry;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.*;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.SECRETSMANAGER;
import static software.amazon.awssdk.services.dynamodb.model.TableStatus.ACTIVE;

import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import it.pagopa.pnss.repositorymanager.entity.ScadenzaDocumentiEntity;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pnss.repositorymanager.entity.TagsRelationsEntity;
import lombok.CustomLog;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.utility.DockerImageName;
import it.pagopa.pnss.configurationproperties.BucketName;
import it.pagopa.pnss.configurationproperties.RepositoryManagerDynamoTableName;
import it.pagopa.pnss.repositorymanager.entity.DocumentEntity;
import it.pagopa.pnss.repositorymanager.entity.UserConfigurationEntity;
import it.pagopa.pnss.testutils.annotation.exception.DynamoDbInitTableCreationException;
import software.amazon.awssdk.core.internal.waiters.ResponseOrException;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.DescribeTableResponse;
import software.amazon.awssdk.services.dynamodb.model.ResourceNotFoundException;
import software.amazon.awssdk.services.dynamodb.waiters.DynamoDbWaiter;
import software.amazon.awssdk.services.s3.S3Client;

@TestConfiguration
@CustomLog
@Import({LocalStackClientConfig.class})
public class LocalStackTestConfig {

    @Autowired
    private DynamoDbClient dynamoDbClient;

    @Autowired
    private DynamoDbEnhancedClient dynamoDbEnhancedClient;

    @Autowired
    private S3Client s3Client;

    @Autowired
    private DynamoDbWaiter dynamoDbWaiter;

    @Autowired
    private RepositoryManagerDynamoTableName repositoryManagerDynamoTableName;

    @Autowired
    private BucketName bucketName;


    static LocalStackContainer localStackContainer = new LocalStackContainer(DockerImageName.parse(DEFAULT_LOCAL_STACK_TAG)).withServices(
            SQS,
            DYNAMODB,
            S3,
            SECRETSMANAGER,
            KINESIS,
            CLOUDWATCH,
            SSM).withEnv("AWS_DEFAULT_REGION", "eu-central-1");

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
        System.setProperty("aws.region", localStackContainer.getRegion());
        System.setProperty("aws.access.key", localStackContainer.getAccessKey());
        System.setProperty("aws.secret.key", localStackContainer.getSecretKey());
        System.setProperty("test.event.bridge", "true");
        System.setProperty("test.aws.secretsmanager.endpoint", String.valueOf(localStackContainer.getEndpointOverride(SECRETSMANAGER)));
        System.setProperty("test.aws.kinesis.endpoint", String.valueOf(localStackContainer.getEndpointOverride(KINESIS)));
        System.setProperty("test.aws.cloudwatch.endpoint", String.valueOf(localStackContainer.getEndpointOverride(CLOUDWATCH)));
        System.setProperty("test.aws.ssm.endpoint", String.valueOf(localStackContainer.getEndpointOverride(SSM)));
        // Prendiamo l'endpoint override di SSM, in quanto EVENT BRIDGE non risulta disponibile nella enum.
        // Fix temporanea, in quanto questa logica andrà cambiata con le modifiche di localdev (PN-13370)
        System.setProperty("test.aws.eventbridge.endpoint", String.valueOf(localStackContainer.getEndpointOverride(SSM)));

        try {
            initS3(localStackContainer);
            initIndexingConfigurationSsm();
            initTransformationConfigSsm();

            //Set Aruba secret credentials.
            localStackContainer.execInContainer("awslocal",
                    "secretsmanager",
                    "create-secret",
                    "--name",
                    "Pn-SS-SignAndTimemark",
                    "--secret-string",
                    getArubaCredentials()

            );

            //Set Namirial secret credentials.
            setNamirialCredentials();

            //Create SQS queue
            for (String queueName : ALL_QUEUE_NAME_LIST) {
                localStackContainer.execInContainer("awslocal", "sqs", "create-queue", "--queue-name", queueName);
            }

        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }

        initParameterStore();
    }

    private static String getArubaCredentials() {
        try {
            return new JSONObject().put("aruba.sign.delegated.domain", "demoprod")
                    .put("aruba.sign.delegated.password", "password11")
                    .put("aruba.sign.delegated.user", "delegato")
                    .put("aruba.sign.otp.pwd", "dsign")
                    .put("aruba.sign.type.otp.auth", "demoprod")
                    .put("aruba.sign.user", "titolare_aut")
                    .put("aruba.timemark.user", "user1")
                    .put("aruba.timemark.password", "password1").toString();
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    private static void initParameterStore() {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            String dimensionsJsonSchema = objectMapper.readTree(LocalStackTestConfig.class.getClassLoader().getResource("json/sign-dimensions-schema-test.json")).toString();
            localStackContainer.execInContainer("awslocal",
                    "ssm",
                    "put-parameter",
                    "--name",
                    "Pn-SS-SignAndTimemark-MetricsSchema",
                    "--type",
                    "String",
                    "--value",
                    dimensionsJsonSchema);
            log.debug("Created parameter Pn-SS-SignAndTimemark-MetricsSchema");
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }


    private static void setNamirialCredentials(){
        System.setProperty("namirial.server.apikey", "namirial-api-key");
    }

    private static String getIdentityTimemarkCredentials() {
        try {
            return new JSONObject().put("user", "user1")
                    .put("password", "password1")
                    .toString();
        } catch (JSONException e) {
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

    private void initDynamoDb() {
        Map<String, Class<?>> tableNameWithEntityClass =
                Map.ofEntries(entry(repositoryManagerDynamoTableName.anagraficaClientName(), UserConfigurationEntity.class),
                        entry(repositoryManagerDynamoTableName.tipologieDocumentiName(), it.pagopa.pnss.repositorymanager.entity.DocTypeEntity.class),
                        entry(repositoryManagerDynamoTableName.documentiName(), DocumentEntity.class),
                        entry(repositoryManagerDynamoTableName.scadenzaDocumentiName(), ScadenzaDocumentiEntity.class),
                        entry(repositoryManagerDynamoTableName.tagsName(), TagsRelationsEntity.class));

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

    private static void initS3(LocalStackContainer localStackContainer) throws IOException, InterruptedException {

        String objectLockConfiguration = "{\"ObjectLockEnabled\":\"Enabled\",\"Rule\":{\"DefaultRetention\":{\"Mode\":\"GOVERNANCE\",\"Days\":1}}}";
        String lifecycleRule = "{\"Rules\": [{\"ID\": \"MoveToGlacier\", \"Filter\": {\"Prefix\": \"\"}, \"Status\": \"Enabled\", \"Transitions\": [{\"Days\": 1, \"StorageClass\": \"GLACIER\"}]}]}";

        List<String> bucketNames = List.of("pn-ss-storage-safestorage", "pn-ss-storage-safestorage-staging");

        bucketNames.forEach(bucket -> {
            try {
                log.info("<-- START S3 init-->");
                Container.ExecResult result = localStackContainer.execInContainer("awslocal", "s3api", "head-bucket", "--bucket", bucket);
                if (result.getStderr().contains("404")) {
                    execInContainer("awslocal", "s3api", "create-bucket", "--region", localStackContainer.getRegion(), "--bucket", bucket, "--object-lock-enabled-for-bucket");
                    execInContainer("awslocal", "s3api", "put-object-lock-configuration", "--bucket", bucket, "--object-lock-configuration", objectLockConfiguration);
                    execInContainer("awslocal", "s3api", "put-bucket-lifecycle-configuration", "--bucket", bucket, "--lifecycle-configuration", lifecycleRule);
                    log.info("New bucket " + bucket + " created on local stack S3");
                } else log.info("Bucket " + bucket + " already created on local stack S3");
            } catch (IOException | InterruptedException ex) {
                ex.printStackTrace();
            }
        });

        Container.ExecResult result = localStackContainer.execInContainer("awslocal", "s3api", "head-object", "--region", localStackContainer.getRegion(), "--bucket", "pn-ss-storage-safestorage", "--key", "ignored-update-metadata.csv");
        String ignoredFileKey = "ignoredFileKey";
        System.setProperty("ignored.file.key", ignoredFileKey);
        if (result.getStderr().contains("404")) {
            execInContainer("awslocal", "s3api", "put-object", "--region", localStackContainer.getRegion(), "--bucket", "pn-ss-storage-safestorage", "--key", "ignored-update-metadata.csv");
        }
    }

    private static void initIndexingConfigurationSsm() throws IOException {
        log.info("<-- START INDEXING CONFIGURATION SSM init-->");
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        var fileReader = new FileReader("src/test/resources/indexing/json/indexing-configuration-default.json");
        Object json = gson.fromJson(fileReader, Object.class);
        String jsonStr = gson.toJson(json);
        try {
            localStackContainer.execInContainer("awslocal",
                    "ssm",
                    "put-parameter",
                    "--name",
                    "Pn-SS-IndexingConfiguration",
                    "--type",
                    "String",
                    "--value",
                    jsonStr);
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private static void initTransformationConfigSsm() throws IOException {
        log.info("<-- START TRANSFORMATION CONFIG SSM init-->");
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        var fileReader = new FileReader("src/test/resources/transformation/transformation-config.json");
        Object json = gson.fromJson(fileReader, Object.class);
        String jsonStr = gson.toJson(json);
        try {
            localStackContainer.execInContainer("awslocal",
                    "ssm",
                    "put-parameter",
                    "--name",
                    "Pn-SS-TransformationConfiguration",
                    "--type",
                    "String",
                    "--value",
                    jsonStr);
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private static void execInContainer(String... command) throws IOException, InterruptedException {
        Container.ExecResult result = localStackContainer.execInContainer(command);
        if (result.getExitCode() != 0) {
            throw new RuntimeException(result.toString());
        }
    }

    @PostConstruct
    public void initLocalStack() {
        initDynamoDb();
    }
}
