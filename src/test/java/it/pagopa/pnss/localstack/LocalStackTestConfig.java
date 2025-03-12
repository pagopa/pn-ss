package it.pagopa.pnss.localstack;

import static org.testcontainers.containers.localstack.LocalStackContainer.Service.*;

import java.io.IOException;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

import java.nio.file.Paths;
import java.time.Duration;

@TestConfiguration
@Import({LocalStackClientConfig.class})
public class LocalStackTestConfig {

    static LocalStackContainer localStack =
            new LocalStackContainer(DockerImageName.parse("localstack/localstack:1.0.4"))
                    .withServices(DYNAMODB)
                    .withCopyFileToContainer(MountableFile.forClasspathResource("testcontainers/config"),
                            "/config")
                    .withClasspathResourceMapping("testcontainers/init.sh",
                            "/docker-entrypoint-initaws.d/make-storages.sh", BindMode.READ_ONLY)
                    .withClasspathResourceMapping("testcontainers/credentials",
                            "/root/.aws/credentials", BindMode.READ_ONLY)
                    .withFileSystemBind(Paths.get("functions").toAbsolutePath().toString(),
                            "/tmp/pn-ss/lambda_import", BindMode.READ_ONLY)
                    .withEnv("AWS_DEFAULT_REGION", "eu-central-1")
                    .withEnv("RUNNING_IN_DOCKER", "true")
                    .withNetworkAliases("localstack")
                    .withNetwork(Network.builder().build())
                    .waitingFor(Wait.forLogMessage(".*Initialization complete.*", 1))
                    .withStartupTimeout(Duration.ofMinutes(5));

    static {
        localStack.start();

        System.setProperty("test.aws.region", localStack.getRegion());

//      <-- Override spring-cloud-starter-aws-messaging endpoints for testing -->
        System.setProperty("cloud.aws.sqs.endpoint", String.valueOf(localStack.getEndpointOverride(SQS)));

//      <-- Override AWS services endpoint variables for testing -->
        System.setProperty("test.aws.sqs.endpoint", String.valueOf(localStack.getEndpointOverride(SQS)));
        System.setProperty("test.aws.dynamodb.endpoint", String.valueOf(localStack.getEndpointOverride(DYNAMODB)));
        System.setProperty("test.aws.sns.endpoint", String.valueOf(localStack.getEndpointOverride(SNS)));
        System.setProperty("test.aws.s3.endpoint", String.valueOf(localStack.getEndpointOverride(S3)));
        System.setProperty("aws.config.access.key", localStack.getAccessKey());
        System.setProperty("aws.config.secret.key", localStack.getSecretKey());
        System.setProperty("aws.config.default.region", localStack.getRegion());
        System.setProperty("aws.region", localStack.getRegion());
        System.setProperty("aws.access.key", localStack.getAccessKey());
        System.setProperty("aws.secret.key", localStack.getSecretKey());
        System.setProperty("test.event.bridge", "true");
        System.setProperty("test.aws.secretsmanager.endpoint", String.valueOf(localStack.getEndpointOverride(SECRETSMANAGER)));
        System.setProperty("test.aws.kinesis.endpoint", String.valueOf(localStack.getEndpointOverride(KINESIS)));
        System.setProperty("test.aws.cloudwatch.endpoint", String.valueOf(localStack.getEndpointOverride(CLOUDWATCH)));
        System.setProperty("test.aws.ssm.endpoint", String.valueOf(localStack.getEndpointOverride(SSM)));
        System.setProperty("aws.endpoint-url", localStack.getEndpointOverride(DYNAMODB).toString());
        // Prendiamo l'endpoint override di SSM, in quanto EVENT BRIDGE non risulta disponibile nella enum.
        System.setProperty("test.aws.eventbridge.endpoint", String.valueOf(localStack.getEndpointOverride(SSM)));

        try {
            System.setProperty("aws.sharedCredentialsFile", new ClassPathResource("testcontainers/credentials").getFile().getAbsolutePath());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
