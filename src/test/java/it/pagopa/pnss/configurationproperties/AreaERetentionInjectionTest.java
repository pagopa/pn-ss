package it.pagopa.pnss.configurationproperties;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AreaERetentionInjectionTest {

    private static final Path RETENTION_SERVICE_IMPL =
            Path.of("src/main/java/it/pagopa/pnss/common/retention/RetentionServiceImpl.java");

    private static final List<String> FORBIDDEN_VALUE_ANNOTATIONS = List.of(
            "@Value(\"${default.internal.x-api-key.value:#{null}}\")",
            "@Value(\"${default.internal.header.x-pagopa-safestorage-cx-id:#{null}}\")",
            "@Value(\"${object.lock.retention.mode}\")",
            "@Value(\"${retention.days.toIgnore}\")");

    @Test
    void retentionServiceImpl_shouldNoLongerDeclareItsDomainValueAnnotations() throws IOException {
        String content = Files.readString(RETENTION_SERVICE_IMPL);
        assertThat(content)
                .as("source file %s", RETENTION_SERVICE_IMPL)
                .doesNotContain(FORBIDDEN_VALUE_ANNOTATIONS.toArray(String[]::new));
    }

    @Test
    void retentionServiceImpl_shouldUsePnSsConfigRetentionSection() throws IOException {
        String content = Files.readString(RETENTION_SERVICE_IMPL);
        assertThat(content)
                .as("source file %s", RETENTION_SERVICE_IMPL)
                .contains("pnSsConfig.getRetention()");
    }
}
