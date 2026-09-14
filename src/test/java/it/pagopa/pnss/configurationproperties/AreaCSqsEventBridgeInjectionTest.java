package it.pagopa.pnss.configurationproperties;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AreaCSqsEventBridgeInjectionTest {

    private static final Path TRANSFORMATION_HANDLER =
            Path.of("src/main/java/it/pagopa/pnss/transformation/handler/TransformationHandler.java");
    private static final Path STREAMS_RECORD_PROCESSOR =
            Path.of("src/main/java/it/pagopa/pnss/availabledocument/event/StreamsRecordProcessor.java");
    private static final Path TRANSFORMATION_SERVICE =
            Path.of("src/main/java/it/pagopa/pnss/transformation/service/TransformationService.java");

    private static final Map<Path, List<String>> FORBIDDEN_VALUE_ANNOTATIONS_BY_FILE = Map.of(
            TRANSFORMATION_HANDLER, List.of(
                    "private String signAndTimemarkQueueName;",
                    "private String signQueueName;"),
            STREAMS_RECORD_PROCESSOR, List.of(
                    "@Value(\"${pn.ss.event-handler.max.messages}\")",
                    "@Value(\"${pn.ss.event-bridge.disponibilita-documenti-name}\")",
                    "@Value(\"${pn.ss.safe-clients}\")"),
            TRANSFORMATION_SERVICE, List.of(
                    "@Value(\"${pn.ss.event-bridge.disponibilita-documenti-name}\")"));

    private static final List<String> RETAINED_ANNOTATION_ATTRIBUTE_PLACEHOLDERS = List.of(
            "@SqsListener(value = \"${pn.ss.transformation.queues.staging}\"",
            "@SqsListener(value = \"${pn.ss.transformation.queues.sign-and-timemark}\"",
            "@SqsListener(value = \"${pn.ss.transformation.queues.sign}\"",
            "@SqsListener(value = \"${pn.ss.transformation.queues.dummy}\"");

    private static final String RETAINED_CRON_ANNOTATION_LITERAL =
            "@Scheduled(cron=\"${PnSsCronStreamsRecordProcessor ?:*/10 * * * * *}\")";

    @Test
    void areaCClasses_shouldNoLongerDeclareTheirDomainValueFieldAnnotations() throws IOException {
        for (Map.Entry<Path, List<String>> entry : FORBIDDEN_VALUE_ANNOTATIONS_BY_FILE.entrySet()) {
            String content = Files.readString(entry.getKey());
            assertThat(content)
                    .as("source file %s", entry.getKey())
                    .doesNotContain(entry.getValue().toArray(String[]::new));
        }
    }

    @Test
    void transformationHandler_shouldUseInjectedTransformationPropertiesAndStillDeclareSqsListenerPlaceholders() throws IOException {
        String content = Files.readString(TRANSFORMATION_HANDLER);
        assertThat(content)
                .as("source file %s", TRANSFORMATION_HANDLER)
                .contains("props.getQueues().getSignAndTimemark()")
                .contains("props.getQueues().getSign()")
                .contains(RETAINED_ANNOTATION_ATTRIBUTE_PLACEHOLDERS.toArray(String[]::new));
    }

    @Test
    void streamsRecordProcessor_cronScheduledAnnotation_shouldPreserveExistingBrokenSeparatorBehaviorUnchanged() throws IOException {
        String content = Files.readString(STREAMS_RECORD_PROCESSOR);
        assertThat(content)
                .as("source file %s", STREAMS_RECORD_PROCESSOR)
                .contains("pnSsConfig.getEventHandler()")
                .contains(RETAINED_CRON_ANNOTATION_LITERAL);
    }
}
