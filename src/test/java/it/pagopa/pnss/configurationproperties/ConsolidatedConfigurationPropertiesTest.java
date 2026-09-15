package it.pagopa.pnss.configurationproperties;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;

class ConsolidatedConfigurationPropertiesTest {

    private static final List<String> CLASSES_CONSOLIDATED_INTO_PN_SS_CONFIG = List.of(
            "it.pagopa.pnss.configurationproperties.BucketName",
            "it.pagopa.pnss.configurationproperties.RepositoryManagerDynamoTableName",
            "it.pagopa.pnss.configurationproperties.StreamRecordProcessorQueueName",
            "it.pagopa.pnss.configurationproperties.AvailabelDocumentEventBridgeName",
            "it.pagopa.pnss.configurationproperties.DynamoEventStreamName",
            "it.pagopa.pnss.common.configurationproperties.endpoint.internal.statemachine.StateMachineEndpointProperties",
            "it.pagopa.pnss.configurationproperties.DynamoRetryStrategyProperties",
            "it.pagopa.pnss.configurationproperties.retry.S3RetryStrategyProperties",
            "it.pagopa.pnss.configurationproperties.GestoreRepositoryRetryStrategyProperties");

    @Test
    void classesConsolidatedIntoPnSsConfig_shouldNoLongerExist() {
        for (String className : CLASSES_CONSOLIDATED_INTO_PN_SS_CONFIG) {
            assertThrows(ClassNotFoundException.class, () -> Class.forName(className),
                    "class " + className + " should have been consolidated into PnSsConfig");
        }
    }
}
