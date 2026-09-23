package it.pagopa.pnss.configurationproperties;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AreaBSignCloudWatchInjectionTest {

    private static final Map<Path, List<String>> FORBIDDEN_VALUE_ANNOTATIONS_BY_FILE = Map.ofEntries(
            Map.entry(Path.of("src/main/java/it/pagopa/pn/library/sign/service/impl/PnSignProviderService.java"), List.of(
                    "@Value(\"${pn.ss.sign.cloudwatch.namespace.aruba}\")",
                    "@Value(\"${pn.ss.sign.cloudwatch.namespace.namirial}\")",
                    "@Value(\"${pn.ss.sign.cloudwatch.metric.response-time.pades}\")",
                    "@Value(\"${pn.ss.sign.cloudwatch.metric.response-time.xades}\")",
                    "@Value(\"${pn.ss.sign.cloudwatch.metric.response-time.cades}\")")),
            Map.entry(Path.of("src/main/java/it/pagopa/pnss/common/service/impl/CloudWatchMetricsService.java"), List.of(
                    "@Value(\"${pn.ss.sign.cloudwatch.metric.dimension.file-size-range}\")",
                    "@Value(\"${pn.ss.sign.cloudwatch.metric.response-time.pades}\")",
                    "@Value(\"${pn.ss.sign.cloudwatch.metric.response-time.xades}\")",
                    "@Value(\"${pn.ss.sign.cloudwatch.metric.response-time.cades}\")")),
            Map.entry(Path.of("src/main/java/it/pagopa/pnss/configuration/cloudwatch/MetricsDimensionConfiguration.java"), List.of(
                    "@Value(\"${pn.ss.sign.cloudwatch.metric.dimension.file-size-range}\")",
                    "@Value(\"${pn.ss.sign.dimension.metrics.schema}\")")),
            Map.entry(Path.of("src/main/java/it/pagopa/pnss/configuration/cloudwatch/CloudWatchMetricPublisherConfiguration.java"), List.of(
                    "@Value(\"${pn.ss.sign.cloudwatch.namespace.aruba}\")",
                    "@Value(\"${pn.ss.sign.cloudwatch.namespace.namirial}\")",
                    "@Value(\"${pn.ss.sign.cloudwatch.metric.response-time.pades}\")",
                    "@Value(\"${pn.ss.sign.cloudwatch.metric.response-time.xades}\")",
                    "@Value(\"${pn.ss.sign.cloudwatch.metric.response-time.cades}\")",
                    "@Value(\"${pn.ss.sign.cloudwatch.metric.dimension.file-size-range}\")",
                    "@Value(\"${pn.ss.sign.cloudwatch.publisher.maximum-calls-per-upload:#{null}}\")",
                    "@Value(\"${pn.ss.sign.cloudwatch.publisher.upload-frequency-millis:#{null}}\")")),
            Map.entry(Path.of("src/main/java/it/pagopa/pnss/configuration/secret/PnSignCredentialConf.java"), List.of(
                    "@Value(\"${pn.ss.identity.signature}\")")));

    private static final List<Path> FILES_THAT_MUST_INJECT_PN_SS_CONFIG = List.of(
            Path.of("src/main/java/it/pagopa/pn/library/sign/service/impl/PnSignProviderService.java"),
            Path.of("src/main/java/it/pagopa/pnss/common/service/impl/CloudWatchMetricsService.java"),
            Path.of("src/main/java/it/pagopa/pnss/configuration/cloudwatch/MetricsDimensionConfiguration.java"),
            Path.of("src/main/java/it/pagopa/pnss/configuration/cloudwatch/CloudWatchMetricPublisherConfiguration.java"),
            Path.of("src/main/java/it/pagopa/pnss/configuration/secret/PnSignCredentialConf.java"));

    private static final Path PN_SIGN_CREDENTIAL_CONF = Path.of("src/main/java/it/pagopa/pnss/configuration/secret/PnSignCredentialConf.java");

    private static final List<String> PN_SIGN_CREDENTIAL_CONF_MOTIVATED_EXCEPTIONS = List.of(
            "@Value(\"${aws.region-code}\")",
            "@Value(\"${test.secret.properties:#{null}}\")",
            "@Value(\"${test.aws.secretsmanager.endpoint:#{null}}\")");

    @Test
    void areaBClasses_shouldNoLongerDeclareTheirDomainValueAnnotations() throws IOException {
        for (Map.Entry<Path, List<String>> entry : FORBIDDEN_VALUE_ANNOTATIONS_BY_FILE.entrySet()) {
            String content = Files.readString(entry.getKey());
            assertThat(content)
                    .as("source file %s", entry.getKey())
                    .doesNotContain(entry.getValue().toArray(String[]::new));
        }
    }

    @Test
    void areaBClasses_shouldInjectPnSsConfigAndPnSignCredentialConfShouldKeepMotivatedExceptions() throws IOException {
        for (Path sourceFile : FILES_THAT_MUST_INJECT_PN_SS_CONFIG) {
            String content = Files.readString(sourceFile);
            assertThat(content)
                    .as("source file %s", sourceFile)
                    .contains("PnSsConfig");
        }
        String pnSignCredentialConfContent = Files.readString(PN_SIGN_CREDENTIAL_CONF);
        assertThat(pnSignCredentialConfContent)
                .as("source file %s", PN_SIGN_CREDENTIAL_CONF)
                .contains(PN_SIGN_CREDENTIAL_CONF_MOTIVATED_EXCEPTIONS.toArray(String[]::new));
    }
}
