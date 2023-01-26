package it.pagopa.pnss.localstack;

public class LocalStackUtils {

    static final String DEFAULT_LOCAL_STACK_TAG = "localstack/localstack:1.0.4";

    static String[] createQueueCliCommand(String queueName) {
        return new String[]{"awslocal", "sqs", "create-queue", "--queue-name", queueName};
    }
}
