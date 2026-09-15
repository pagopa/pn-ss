package it.pagopa.pnss.configurationproperties;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AreaFUriBuilderInjectionTest {

    private static final Path URI_BUILDER_SERVICE =
            Path.of("src/main/java/it/pagopa/pnss/uribuilder/service/UriBuilderService.java");

    private static final List<String> FORBIDDEN_VALUE_ANNOTATIONS = List.of(
            "@Value(\"${uri.builder.presigned.url.duration.minutes.download}\")",
            "@Value(\"${uri.builder.presigned.url.duration.minutes.upload}\")",
            "@Value(\"${uri.builder.stay.Hot.Bucket.tyme.days}\")",
            "@Value(\"${header.presignUrl.checksum-sha256:#{null}}\")",
            "@Value(\"${presignedUrl.initial.newDocument.state}\")",
            "@Value(\"${queryParam.presignedUrl.traceId:#{null}}\")",
            "@Value(\"${max.restore.time.cold}\")",
            "@Value(\"${default.internal.x-api-key.value:#{null}}\")",
            "@Value(\"${default.internal.header.x-pagopa-safestorage-cx-id:#{null}}\")",
            "@Value(\"${amz.restore.request.date.header.name}\")",
            "@Value(\"${uri.builder.get.file.with.patch.configuration}\")");

    private static final String RETAINED_TEST_AWS_EXCEPTION =
            "@Value(\"${test.aws.s3.endpoint:#{null}}\")";

    @Test
    void uriBuilderService_shouldNoLongerDeclareItsDomainValueAnnotationsAndShouldUsePnSsConfig() throws IOException {
        String content = Files.readString(URI_BUILDER_SERVICE);
        assertThat(content)
                .as("source file %s", URI_BUILDER_SERVICE)
                .doesNotContain(FORBIDDEN_VALUE_ANNOTATIONS.toArray(String[]::new))
                .contains("pnSsConfig.getUriBuilder()")
                .contains(RETAINED_TEST_AWS_EXCEPTION);
    }
}
