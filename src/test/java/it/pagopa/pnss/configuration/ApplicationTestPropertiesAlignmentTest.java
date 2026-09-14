package it.pagopa.pnss.configuration;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

class ApplicationTestPropertiesAlignmentTest {

    private static final Path MAIN_APPLICATION_PROPERTIES = Path.of("src/main/resources/application.properties");

    private static final Path TEST_APPLICATION_PROPERTIES = Path.of("src/test/resources/application-test.properties");

    private static final Pattern KEY_VALUE_LINE_PATTERN = Pattern.compile("^([^#!\\s][^=]*?)\\s*=\\s*(.*)$");

    private static final Pattern BARE_PLACEHOLDER_PATTERN = Pattern.compile("^\\$\\{[A-Za-z0-9._-]+}$");

    private static final Map<String, String> REAL_TESTCONTAINERS_BUCKET_NAME_BY_KEY = buildRealBucketNamesByKey();

    private static Map<String, String> buildRealBucketNamesByKey() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("pn.ss.bucket.hot-name", "pn-ss-storage-safestorage");
        map.put("pn.ss.bucket.stage-name", "pn-ss-storage-safestorage-staging");
        return map;
    }

    @Test
    void everyMainKeyWithoutDefault_shouldHaveConcreteOverrideInTestProperties() throws IOException {
        Map<String, String> mainProperties = readProperties(MAIN_APPLICATION_PROPERTIES);
        Map<String, String> testProperties = readProperties(TEST_APPLICATION_PROPERTIES);

        List<String> keysRequiringExternalResolution = mainProperties.entrySet()
                .stream()
                .filter(entry -> BARE_PLACEHOLDER_PATTERN.matcher(entry.getValue()).matches())
                .map(Map.Entry::getKey)
                .toList();

        assertThat(keysRequiringExternalResolution)
                .as("sanity check: application.properties must contain at least one key without a default")
                .isNotEmpty();

        for (String key : keysRequiringExternalResolution) {
            String testValue = testProperties.get(key);
            assertThat(testValue)
                    .as("property '%s' has no default in application.properties, so application-test.properties " +
                            "must define a concrete override for it", key)
                    .isNotNull();
            assertThat(BARE_PLACEHOLDER_PATTERN.matcher(testValue).matches())
                    .as("property '%s' in application-test.properties must resolve to a concrete value, " +
                            "not remain an unresolved placeholder ('%s')", key, testValue)
                    .isFalse();
        }
    }

    @Test
    void bucketNames_shouldBeOverriddenInTestPropertiesWithRealTestcontainersBucketNames() throws IOException {
        Map<String, String> testProperties = readProperties(TEST_APPLICATION_PROPERTIES);

        REAL_TESTCONTAINERS_BUCKET_NAME_BY_KEY.forEach((key, expectedRealBucketName) -> {
            String testValue = testProperties.get(key);
            assertThat(testValue)
                    .as("property '%s' must be overridden in application-test.properties with the real bucket name " +
                            "created by src/test/resources/testcontainers/init.sh", key)
                    .isEqualTo(expectedRealBucketName);
        });
    }

    private Map<String, String> readProperties(Path path) throws IOException {
        Map<String, String> properties = new LinkedHashMap<>();
        List<String> lines = Files.readAllLines(path, StandardCharsets.ISO_8859_1);
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#") || trimmed.startsWith("!")) {
                continue;
            }
            Matcher matcher = KEY_VALUE_LINE_PATTERN.matcher(trimmed);
            if (matcher.matches()) {
                properties.put(matcher.group(1).trim(), matcher.group(2).trim());
            }
        }
        return properties;
    }
}
