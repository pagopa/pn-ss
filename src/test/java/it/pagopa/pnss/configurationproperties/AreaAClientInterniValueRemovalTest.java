package it.pagopa.pnss.configurationproperties;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AreaAClientInterniValueRemovalTest {

    private static final Map<Path, List<String>> FORBIDDEN_VALUE_ANNOTATIONS_BY_FILE = Map.ofEntries(
            Map.entry(Path.of("src/main/java/it/pagopa/pnss/common/client/impl/DocumentClientCallImpl.java"), List.of(
                    "@Value(\"${gestore.repository.anagrafica.internal.docClient}\")",
                    "@Value(\"${gestore.repository.anagrafica.internal.docClient.post}\")",
                    "@Value(\"${header.x-api-key}\")",
                    "@Value(\"${header.x-pagopa-safestorage-cx-id}\")")),
            Map.entry(Path.of("src/main/java/it/pagopa/pnss/common/client/impl/UserConfigurationClientCallImpl.java"), List.of(
                    "@Value(\"${gestore.repository.anagrafica.internal.userConfiguration}\")")),
            Map.entry(Path.of("src/main/java/it/pagopa/pnss/common/client/impl/ScadenzaDocumentiClientCallImpl.java"), List.of(
                    "@Value(\"${gestore.repository.anagrafica.internal.scadenza.documenti.post}\")")),
            Map.entry(Path.of("src/main/java/it/pagopa/pnss/common/client/impl/ConfigurationApiCallImpl.java"), List.of(
                    "@Value(\"${header.x-api-key}\")",
                    "@Value(\"${header.x-pagopa-safestorage-cx-id}\")",
                    "@Value(\"${gestore.repository.configuration.api.documents.config}\")")),
            Map.entry(Path.of("src/main/java/it/pagopa/pnss/common/client/impl/DocTypesClientCallImpl.java"), List.of(
                    "@Value(\"${gestore.repository.anagrafica.internal.docTypes}\")")),
            Map.entry(Path.of("src/main/java/it/pagopa/pnss/common/client/impl/TagsClientCallImpl.java"), List.of(
                    "@Value(\"${header.x-pagopa-safestorage-cx-id}\")",
                    "@Value(\"${gestore.repository.anagrafica.internal.tags.get}\")",
                    "@Value(\"${gestore.repository.anagrafica.internal.tags.put}\")")),
            Map.entry(Path.of("src/main/java/it/pagopa/pnss/uribuilder/rest/FileMetadataUpdateApiController.java"), List.of(
                    "@Value(\"${header.x-api-key}\")",
                    "@Value(\"${header.x-pagopa-safestorage-cx-id}\")")),
            Map.entry(Path.of("src/main/java/it/pagopa/pnss/uribuilder/rest/FileDownloadApiController.java"), List.of(
                    "@Value(\"${queryParam.presignedUrl.traceId}\")")),
            Map.entry(Path.of("src/main/java/it/pagopa/pnss/uribuilder/rest/FileUploadApiController.java"), List.of(
                    "@Value(\"${queryParam.presignedUrl.traceId}\")")),
            Map.entry(Path.of("src/main/java/it/pagopa/pnss/configuration/security/SecurityConfiguration.java"), List.of(
                    "@Value(\"${header.x-api-key}\")",
                    "@Value(\"${header.x-pagopa-safestorage-cx-id}\")")),
            Map.entry(Path.of("src/main/java/it/pagopa/pnss/repositorymanager/rest/internal/DocumentInternalApiController.java"), List.of(
                    "@Value(\"${header.x-api-key}\")",
                    "@Value(\"${header.x-pagopa-safestorage-cx-id}\")")),
            Map.entry(Path.of("src/main/java/it/pagopa/pnss/configuration/http/WebClientConf.java"), List.of(
                    "@Value(\"${internal.base.url}\")",
                    "@Value(\"${pn.log.cx-id-header}\")")),
            Map.entry(Path.of("src/main/java/it/pagopa/pnss/configuration/http/JettyHttpClientConf.java"), List.of(
                    "@Value(\"${jetty.maxConnectionsPerDestination}\")",
                    "@Value(\"${pn.log.cx-id-header}\")")));

    private static final List<Path> FILES_THAT_MUST_INJECT_PN_SS_CONFIG = List.of(
            Path.of("src/main/java/it/pagopa/pnss/common/client/impl/DocumentClientCallImpl.java"),
            Path.of("src/main/java/it/pagopa/pnss/common/client/impl/UserConfigurationClientCallImpl.java"),
            Path.of("src/main/java/it/pagopa/pnss/common/client/impl/ScadenzaDocumentiClientCallImpl.java"),
            Path.of("src/main/java/it/pagopa/pnss/common/client/impl/ConfigurationApiCallImpl.java"),
            Path.of("src/main/java/it/pagopa/pnss/common/client/impl/DocTypesClientCallImpl.java"),
            Path.of("src/main/java/it/pagopa/pnss/common/client/impl/TagsClientCallImpl.java"),
            Path.of("src/main/java/it/pagopa/pnss/uribuilder/rest/FileMetadataUpdateApiController.java"),
            Path.of("src/main/java/it/pagopa/pnss/uribuilder/rest/FileDownloadApiController.java"),
            Path.of("src/main/java/it/pagopa/pnss/uribuilder/rest/FileUploadApiController.java"),
            Path.of("src/main/java/it/pagopa/pnss/configuration/security/SecurityConfiguration.java"),
            Path.of("src/main/java/it/pagopa/pnss/repositorymanager/rest/internal/DocumentInternalApiController.java"),
            Path.of("src/main/java/it/pagopa/pnss/configuration/http/JettyHttpClientConf.java"));

    @Test
    void areaAClasses_shouldNoLongerDeclareTheirDomainValueAnnotations() throws IOException {
        for (Map.Entry<Path, List<String>> entry : FORBIDDEN_VALUE_ANNOTATIONS_BY_FILE.entrySet()) {
            String content = Files.readString(entry.getKey());
            assertThat(content)
                    .as("source file %s", entry.getKey())
                    .doesNotContain(entry.getValue().toArray(String[]::new));
        }
    }

    @Test
    void areaAClasses_shouldInjectPnSsConfig() throws IOException {
        for (Path sourceFile : FILES_THAT_MUST_INJECT_PN_SS_CONFIG) {
            String content = Files.readString(sourceFile);
            assertThat(content)
                    .as("source file %s", sourceFile)
                    .contains("PnSsConfig");
        }
    }
}
