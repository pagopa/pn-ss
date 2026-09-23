package it.pagopa.pnss.configurationproperties;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AreaGAwsConfigurationExceptionsTest {

    private static final Path AWS_CONFIGURATION =
            Path.of("src/main/java/it/pagopa/pnss/configuration/AwsConfiguration.java");

    private static final List<String> MOTIVATED_UNCHANGED_VALUE_FIELD_NAMES = List.of(
            "regionCode",
            "localStackRegion",
            "sqsLocalStackEndpoint",
            "dynamoDbLocalStackEndpoint",
            "snsLocalStackEndpoint",
            "eventBridgeLocalStackEndpoint",
            "testEventBridge",
            "testAwsS3Endpoint",
            "testAwsCloudwatchEndpoint",
            "testAwsSsmEndpoint");

    @Test
    void awsConfiguration_shouldStillDeclareExactlyTheTenMotivatedValueFields() throws ClassNotFoundException {
        Class<?> awsConfigurationClass = Class.forName("it.pagopa.pnss.configuration.AwsConfiguration");
        long valueAnnotatedFieldCount = 0;
        for (Field field : awsConfigurationClass.getDeclaredFields()) {
            if (field.isAnnotationPresent(org.springframework.beans.factory.annotation.Value.class)) {
                valueAnnotatedFieldCount++;
                assertThat(MOTIVATED_UNCHANGED_VALUE_FIELD_NAMES)
                        .as("unexpected @Value field '%s' found in AwsConfiguration, outside the motivated exception list", field.getName())
                        .contains(field.getName());
            }
        }
        assertThat(valueAnnotatedFieldCount).isEqualTo(MOTIVATED_UNCHANGED_VALUE_FIELD_NAMES.size());
    }

    @Test
    void awsConfiguration_shouldNotInjectPnSsConfig() throws IOException {
        String content = Files.readString(AWS_CONFIGURATION);
        assertThat(content)
                .as("source file %s : area G is entirely out of scope for this task (future SRS #4)", AWS_CONFIGURATION)
                .doesNotContain("PnSsConfig");
    }
}
