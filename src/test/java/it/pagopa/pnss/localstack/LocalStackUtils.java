package it.pagopa.pnss.localstack;

import org.springframework.context.annotation.Bean;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;

import java.net.URI;

public class LocalStackUtils {

    static final String DEFAULT_LOCAL_STACK_TAG = "localstack/localstack:1.0.4";

    static String[] createQueueCliCommand(String queueName) {
        return new String[]{"awslocal", "sqs", "create-queue", "--queue-name", queueName};
    }


}
