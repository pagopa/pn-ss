package it.pagopa.pnss.configuration;

import it.pagopa.TemplateApplication;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.PropertySource;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PropertiesStructureTest {

    private static final Path MAIN_RESOURCES = Path.of("src/main/resources");

    private static final List<String> OLD_NON_UNIFORM_ROOT_PREFIXES = List.of(
            "s3.bucket.",
            "dynamo.table.repository-manager.",
            "sqs.queue.availability.",
            "internal-endpoint.state-machine.",
            "event.bridge.",
            "dynamo.event.stream.",
            "namirial.server.",
            "pn.sign.",
            "dynamo.retry.strategy.",
            "gestore.repository.retry.strategy.",
            "s3.retry.strategy.");

    private static final List<String> FORBIDDEN_REAL_BUCKET_VALUES = List.of(
            "pn-ss-storage-safestorage-eu-central-1-713024823233",
            "pn-ss-storage-safestorage-staging-eu-central-1-713024823233",
            "dgs-bing-ss-pnssbucket-27myu2kp62x9");

    @Test
    void deadPropertiesFiles_shouldNotExistUnderMainResources() {
        assertThat(new File("src/main/resources/DocumentConfig.properties")).doesNotExist();
        assertThat(new File("src/main/resources/UserConfig.properties")).doesNotExist();
    }

    @Test
    void queueNameDeadClass_shouldNoLongerExist() {
        assertThrows(ClassNotFoundException.class,
                () -> Class.forName("it.pagopa.pnss.configurationproperties.QueueName"));
    }

    @Test
    void mainResources_shouldContainOnlyOnePropertiesFile() throws IOException {
        List<Path> propertiesFiles;
        try (var stream = Files.walk(MAIN_RESOURCES)) {
            propertiesFiles = stream.filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".properties"))
                    .toList();
        }
        assertThat(propertiesFiles).hasSize(1);
        assertThat(propertiesFiles.get(0).getFileName().toString()).isEqualTo("application.properties");
    }

    @Test
    void noPropertiesFile_shouldContainFallbackToRealBuckets() throws IOException {
        String allPropertiesContent = readAllPropertiesFilesContent();
        assertThat(allPropertiesContent).doesNotContain(FORBIDDEN_REAL_BUCKET_VALUES.toArray(String[]::new));
    }

    @Test
    void noPropertiesFile_shouldContainOldNonUniformRootKeys() throws IOException {
        List<String> lines;
        try (var stream = Files.walk(MAIN_RESOURCES)) {
            lines = stream.filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".properties"))
                    .flatMap(path -> {
                        try {
                            return Files.readAllLines(path, StandardCharsets.ISO_8859_1).stream();
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }
                    })
                    .map(String::trim)
                    .toList();
        }

        for (String prefix : OLD_NON_UNIFORM_ROOT_PREFIXES) {
            boolean found = lines.stream().anyMatch(line -> line.startsWith(prefix));
            assertThat(found)
                    .as("no residual line should start with old non-uniform root prefix '%s'", prefix)
                    .isFalse();
        }
    }

    @Test
    void templateApplication_shouldNotDeclareAnyPropertySource() {
        PropertySource[] propertySources = TemplateApplication.class.getAnnotationsByType(PropertySource.class);
        assertThat(propertySources).isEmpty();
    }

    private String readAllPropertiesFilesContent() throws IOException {
        StringBuilder builder = new StringBuilder();
        try (var stream = Files.walk(MAIN_RESOURCES)) {
            for (Path path : stream.filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".properties"))
                    .toList()) {
                builder.append(Files.readString(path, StandardCharsets.ISO_8859_1)).append('\n');
            }
        }
        return builder.toString();
    }
}
