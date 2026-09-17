package it.pagopa.pnss.configurationproperties;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AreaDTransformationIndexingInjectionTest {

    private static final Path TRANSFORMATION_CONFIG =
            Path.of("src/main/java/it/pagopa/pnss/configuration/TransformationConfig.java");
    private static final Path INDEXING_CONFIGURATION =
            Path.of("src/main/java/it/pagopa/pnss/configuration/IndexingConfiguration.java");
    private static final Path IGNORED_UPDATE_METADATA_CONFIG =
            Path.of("src/main/java/it/pagopa/pnss/configuration/IgnoredUpdateMetadataConfig.java");
    private static final Path DOCUMENT_SERVICE_IMPL =
            Path.of("src/main/java/it/pagopa/pnss/repositorymanager/service/impl/DocumentServiceImpl.java");

    private static final Map<Path, List<String>> FORBIDDEN_VALUE_ANNOTATIONS_BY_FILE = Map.of(
            TRANSFORMATION_CONFIG, List.of(
                    "@Value(\"${pn.ss.transformation.config.parameter-name}\")"),
            INDEXING_CONFIGURATION, List.of(
                    "@Value(\"${pn.ss.indexing.configuration.name}\")"),
            IGNORED_UPDATE_METADATA_CONFIG, List.of(
                    "@Value(\"${pn.ss.ignored.update.metadata.list}\") String ignoredUpdateMetadataListUri"),
            DOCUMENT_SERVICE_IMPL, List.of(
                    "@Value(\"${pn.ss.indexing.document-number-of-pages-tag-key}\")"));

    @Test
    void areaDClasses_shouldNoLongerDeclareTheirDomainValueAnnotations() throws IOException {
        for (Map.Entry<Path, List<String>> entry : FORBIDDEN_VALUE_ANNOTATIONS_BY_FILE.entrySet()) {
            String content = Files.readString(entry.getKey());
            assertThat(content)
                    .as("source file %s", entry.getKey())
                    .doesNotContain(entry.getValue().toArray(String[]::new));
        }
    }

    @Test
    void transformationConfig_shouldInjectExistingTransformationPropertiesInsteadOfRawValue() throws IOException {
        String content = Files.readString(TRANSFORMATION_CONFIG);
        assertThat(content)
                .as("source file %s : TransformationProperties.Config.parameterName already carries this property, no need to duplicate it in PnSsConfig", TRANSFORMATION_CONFIG)
                .contains("TransformationProperties");
    }

    @Test
    void indexingAndIgnoredUpdateMetadataAndDocumentService_shouldInjectPnSsConfig() throws IOException {
        for (Path sourceFile : List.of(INDEXING_CONFIGURATION, IGNORED_UPDATE_METADATA_CONFIG)) {
            String content = Files.readString(sourceFile);
            assertThat(content)
                    .as("source file %s", sourceFile)
                    .contains("PnSsConfig");
        }
        // DocumentServiceImpl already injects PnSsConfig; it must now also use it for the tag key.
        String documentServiceContent = Files.readString(DOCUMENT_SERVICE_IMPL);
        assertThat(documentServiceContent)
                .as("source file %s", DOCUMENT_SERVICE_IMPL)
                .contains("pnSsConfig.getIndexing().getDocumentNumberOfPagesTagKey()");
    }
}
