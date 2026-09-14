package it.pagopa.pnss.configuration.cloudwatch;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CloudWatchPropertyKeysTest {

    private static final List<Path> CLOUDWATCH_NAMESPACE_SOURCE_FILES = List.of(
            Path.of("src/main/java/it/pagopa/pnss/configuration/cloudwatch/MetricsDimensionConfiguration.java"),
            Path.of("src/main/java/it/pagopa/pnss/configuration/cloudwatch/CloudWatchMetricPublisherConfiguration.java"),
            Path.of("src/main/java/it/pagopa/pnss/common/service/impl/CloudWatchMetricsService.java"),
            Path.of("src/main/java/it/pagopa/pn/library/sign/service/impl/PnSignProviderService.java"));

    @Test
    void cloudwatchSourceFiles_shouldNoLongerReferenceOldPnSignPrefixAndShouldUsePnSsConfig() throws IOException {
        for (Path sourceFile : CLOUDWATCH_NAMESPACE_SOURCE_FILES) {
            String content = Files.readString(sourceFile);
            assertThat(content)
                    .as("source file %s", sourceFile)
                    .doesNotContain("${pn.sign.cloudwatch")
                    .doesNotContain("${pn.ss.sign.cloudwatch")
                    .contains("PnSsConfig");
        }
    }

    @Test
    void metricsDimensionSchema_shouldNoLongerReferenceOldPnSignPrefixAndShouldUsePnSsConfig() throws IOException {
        String content = Files.readString(
                Path.of("src/main/java/it/pagopa/pnss/configuration/cloudwatch/MetricsDimensionConfiguration.java"));
        assertThat(content)
                .doesNotContain("${pn.sign.dimension.metrics.schema}")
                .doesNotContain("${pn.ss.sign.dimension.metrics.schema}")
                .contains("PnSsConfig");
    }
}
