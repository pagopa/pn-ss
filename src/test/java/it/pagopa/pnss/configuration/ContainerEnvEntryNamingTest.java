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

class ContainerEnvEntryNamingTest {

    private static final Path MICROSERVICE_CFN_TEMPLATE = Path.of("scripts/aws/cfn/microservice.yml");

    private static final Pattern CONTAINER_ENV_ENTRY_LINE_PATTERN =
            Pattern.compile("^\\s*ContainerEnvEntry(\\d+):\\s*(?:!Sub\\s+)?'([^='\\n]+)=(.*)'\\s*$");

    private static final Pattern UPPER_SNAKE_CASE_PATTERN = Pattern.compile("^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$");

    private static final int EXPECTED_TOTAL_ENTRY_COUNT = 58;

    private static final Map<Integer, String> ALREADY_COMPLIANT_ENTRIES = buildAlreadyCompliantEntries();

    private static final Map<Integer, String> EXPECTED_RENAMED_KEYS = buildExpectedRenamedKeys();

    private static final Map<Integer, String> EXPECTED_RIGHT_HAND_SIDES = buildExpectedRightHandSides();

    private record ContainerEnvEntry(int number, String key, String rightHandSide) {
    }

    private static Map<Integer, String> buildAlreadyCompliantEntries() {
        Map<Integer, String> map = new LinkedHashMap<>();
        map.put(1, "AWS_REGIONCODE");
        map.put(69, "PN_CRON_ANALYZER");
        map.put(70, "WIRE_TAP_LOG");
        return map;
    }

    private static Map<Integer, String> buildExpectedRenamedKeys() {
        Map<Integer, String> map = new LinkedHashMap<>();
        map.put(2, "PN_SS_EVENT_BUS_NAME_EXTERNAL_NOTIFICATION");
        map.put(3, "PN_SS_BUCKET_NAME");
        map.put(4, "PN_SS_BUCKET_ARN");
        map.put(5, "PN_SS_STAGING_BUCKET_NAME");
        map.put(6, "PN_SS_STAGING_BUCKET_ARN");
        map.put(7, "PN_SS_TABLE_NAME_ANAGRAFICA_CLIENT");
        map.put(8, "PN_SS_TABLE_NAME_TIPOLOGIE_DOCUMENTI");
        map.put(9, "PN_SS_TABLE_NAME_DOCUMENTI");
        map.put(10, "PN_SS_TABLE_DOCUMENTI_STREAM_ARN");
        map.put(11, "PN_SS_TABLE_DOCUMENTI_STREAM_METADATI_TABLE");
        map.put(12, "PN_SS_QUEUE_NAME_STAGING_BUCKET");
        map.put(19, "DURATION_MINUTES_UPLOAD");
        map.put(20, "DURATION_MINUTES_DOWNLOAD");
        map.put(21, "STAY_HOT_TIME");
        map.put(22, "PN_SS_BUCKET_LOCK_RETENTION_MODE");
        map.put(23, "PN_SS_PRELOADED_DOCS_RETENTION_DAYS");
        map.put(30, "PN_SS_ARUBA_CERT_ID");
        map.put(31, "PN_SS_ARUBA_ENABLED_LOG");
        map.put(32, "PN_SS_ARUBA_SIGN_WSDL_URL");
        map.put(33, "PN_SS_ARUBA_QNAME");
        map.put(34, "PN_SS_ARUBA_SIGN_SERVICE");
        map.put(35, "PN_SS_TSA_IDENTITY");
        map.put(36, "PN_SS_IDENTITY_SIGNATURE");
        map.put(37, "PN_SS_TIMEMARK_URL");
        map.put(38, "PN_SS_URI_BUILDER_GET_FILE_WITH_PATCH_CONFIGURATION");
        map.put(39, "STATE_MACHINE_BASE_URL");
        map.put(40, "INTERNAL_BASE_URL");
        map.put(41, "PN_SS_TASK_EXECUTION_POOL_MAX_SIZE");
        map.put(42, "PN_SIGN_PROVIDER_SWITCH");
        map.put(43, "PN_SS_SIGN_RETRY_STRATEGY_MAX_ATTEMPTS");
        map.put(44, "PN_SS_SIGN_RETRY_STRATEGY_MIN_BACKOFF");
        map.put(45, "PN_SS_NAMIRIAL_SERVER_ADDRESS");
        map.put(46, "PN_EC_NAMIRIAL_SERVER_MAX_CONNECTIONS");
        map.put(47, "PN_EC_NAMIRIAL_SERVER_PENDING_ACQUIRE_TIMEOUT");
        map.put(48, "PN_SS_TABLE_NAME_SCADENZA_DOCUMENTI");
        map.put(49, "PN_SS_TABLE_SCADENZA_DOCUMENTI_STREAM_ARN");
        map.put(50, "PN_SS_IGNORED_UPDATE_METADATA_LIST");
        map.put(51, "PN_SS_INDEXING_CONFIGURATION");
        map.put(52, "PN_SS_TABLE_NAME_TAGS");
        map.put(53, "PN_SS_SAFE_CLIENTS");
        map.put(54, "PN_SS_QUEUE_NAME_AVAILABILITY");
        map.put(55, "PN_SS_EVENT_HANDLER_MAX_MESSAGES");
        map.put(56, "PN_SS_SQS_RETRY_STRATEGY_MIN_BACKOFF");
        map.put(57, "PN_SS_SQS_RETRY_STRATEGY_MAX_ATTEMPTS");
        map.put(58, "SIGN_AND_TIMEMARK_MAX_THREAD_POOL_SIZE");
        map.put(59, "SPRING_CODEC_MAX_IN_MEMORY_SIZE");
        map.put(60, "PN_SS_SIGN_AND_TIMEMARK_METRICS_SCHEMA");
        map.put(61, "PN_SS_TRANSFORMATION_SERVICE_MAX_MESSAGES");
        map.put(62, "PN_SS_TASK_EXECUTION_POOL_CORE_SIZE");
        map.put(63, "PN_SS_TASK_EXECUTION_POOL_QUEUE_CAPACITY");
        map.put(64, "PN_SS_QUEUE_NAME_DUMMY_TRANSFORMATION");
        map.put(65, "PN_SS_QUEUE_NAME_SIGN_AND_TIMEMARK_TRANSFORMATION");
        map.put(66, "PN_SS_QUEUE_NAME_SIGN_TRANSFORMATION");
        map.put(67, "PN_SS_TRANSFORMATION_CONFIGURATION");
        map.put(68, "SIGN_MAX_THREAD_POOL_SIZE");
        return map;
    }

    private static Map<Integer, String> buildExpectedRightHandSides() {
        Map<Integer, String> map = new LinkedHashMap<>();
        map.put(1, "${AWS::Region}");
        map.put(2, "${PnSsEventBusNameExternalNotification}");
        map.put(3, "${PnSsBucketName}");
        map.put(4, "${PnSsBucketArn}");
        map.put(5, "${PnSsStagingBucketName}");
        map.put(6, "${PnSsStagingBucketArn}");
        map.put(7, "${PnSsTableNameAnagraficaClient}");
        map.put(8, "${PnSsTableNameTipologieDocumenti}");
        map.put(9, "${PnSsTableNameDocumenti}");
        map.put(10, "${PnSsTableDocumentiStreamArn}");
        map.put(11, "${PnSsTableDocumentiStreamMetadatiTable}");
        map.put(12, "${PnSsQueueNameStagingBucket}");
        map.put(19, "${DurationMinutesUpload}");
        map.put(20, "${DurationMinutesDownload}");
        map.put(21, "${PnSsUriBuilderstayHotTime}");
        map.put(22, "GOVERNANCE");
        map.put(23, "${PnSsPreloadedDocsRetentionDays}");
        map.put(30, "${PnSsArubaCertId}");
        map.put(31, "${PnSsArubaEnabledLog}");
        map.put(32, "${PnSsArubaSignWsdlUrl}");
        map.put(33, "${PnSsArubaQname}");
        map.put(34, "${PnSsArubaSignService}");
        map.put(35, "${PnSsTsaIdentity}");
        map.put(36, "${PnSsIdentitySignature}");
        map.put(37, "${PnSsTimemarkUrl}");
        map.put(38, "${PnSsUriBuilderGetFileWithPatchConfiguration}");
        map.put(39, "http://${ApplicationLoadBalancerDomain}:8080");
        map.put(40, "http://${ApplicationLoadBalancerDomain}:8080");
        map.put(41, "${PnSsTaskExecutionPoolMaxSize}");
        map.put(42, "${PnSignProviderSwitch}");
        map.put(43, "${PnSsSignRetryStrategyMaxAttempts}");
        map.put(44, "${PnSsSignRetryStrategyMinBackoff}");
        map.put(45, "${PnSsNamirialServerAddress}");
        map.put(46, "${PnEcNamirialServerMaxConnections}");
        map.put(47, "${PnEcNamirialServerPendingAcquireTimeout}");
        map.put(48, "${PnSsTableNameScadenzaDocumenti}");
        map.put(49, "${PnSsTableScadenzaDocumentiStreamArn}");
        map.put(50, "${PnSsIgnoredUpdateMetadataList}");
        map.put(51, "${PnSsIndexingConfiguration}");
        map.put(52, "${PnSsTableNameTags}");
        map.put(53, "${PnSsSafeClients}");
        map.put(54, "${PnSsQueueNameAvailability}");
        map.put(55, "${PnSsEventHandlerMaxMessages}");
        map.put(56, "${PnSsSqsRetryStrategyMinBackoff}");
        map.put(57, "${PnSsSqsRetryStrategyMaxAttempts}");
        map.put(58, "${SignAndTimemarkMaxThreadPoolSize}");
        map.put(59, "${SpringCodecMaxInMemorySize}");
        map.put(60, "${PnSsSignAndTimemarkMetricsSchema}");
        map.put(61, "${PnSsTransformationServiceMaxMessages}");
        map.put(62, "${PnSsTaskExecutionPoolCoreSize}");
        map.put(63, "${PnSsTaskExecutionPoolQueueCapacity}");
        map.put(64, "${PnSsQueueNameDummyTransformation}");
        map.put(65, "${PnSsQueueNameSignAndTimemarkTransformation}");
        map.put(66, "${PnSsQueueNameSignTransformation}");
        map.put(67, "${PnSsTransformationConfiguration}");
        map.put(68, "${SignMaxThreadPoolSize}");
        map.put(69, "${PnCronAnalyzer}");
        map.put(70, "${WireTapLogActivation}");
        return map;
    }

    @Test
    void containerEnvEntryCount_shouldRemainUnchanged() throws IOException {
        List<ContainerEnvEntry> entries = readContainerEnvEntries();
        assertThat(entries).hasSize(EXPECTED_TOTAL_ENTRY_COUNT);
    }

    @Test
    void alreadyCompliantEntries_shouldRemainUnchanged() throws IOException {
        Map<Integer, ContainerEnvEntry> byNumber = indexByNumber(readContainerEnvEntries());
        ALREADY_COMPLIANT_ENTRIES.forEach((number, expectedKey) -> {
            ContainerEnvEntry entry = byNumber.get(number);
            assertThat(entry).as("entry %d must exist", number).isNotNull();
            assertThat(entry.key()).as("entry %d key must stay untouched", number).isEqualTo(expectedKey);
        });
    }

    @Test
    void nonCompliantEntries_shouldBeRenamedToUpperSnakeCase() throws IOException {
        Map<Integer, ContainerEnvEntry> byNumber = indexByNumber(readContainerEnvEntries());
        EXPECTED_RENAMED_KEYS.forEach((number, expectedKey) -> {
            ContainerEnvEntry entry = byNumber.get(number);
            assertThat(entry).as("entry %d must exist", number).isNotNull();
            assertThat(entry.key())
                    .as("entry %d key must be renamed to '%s'", number, expectedKey)
                    .isEqualTo(expectedKey);
            assertThat(UPPER_SNAKE_CASE_PATTERN.matcher(entry.key()).matches())
                    .as("entry %d key '%s' must be UPPER_SNAKE_CASE", number, entry.key())
                    .isTrue();
        });
    }

    @Test
    void allEntries_shouldPreserveOriginalRightHandSide() throws IOException {
        Map<Integer, ContainerEnvEntry> byNumber = indexByNumber(readContainerEnvEntries());
        EXPECTED_RIGHT_HAND_SIDES.forEach((number, expectedRightHandSide) -> {
            ContainerEnvEntry entry = byNumber.get(number);
            assertThat(entry).as("entry %d must exist", number).isNotNull();
            assertThat(entry.rightHandSide())
                    .as("entry %d right-hand side must remain unaltered", number)
                    .isEqualTo(expectedRightHandSide);
        });
    }

    @Test
    void renamedKeys_shouldAllBeDistinct() {
        assertThat(EXPECTED_RENAMED_KEYS.values()).doesNotHaveDuplicates();
    }

    private List<ContainerEnvEntry> readContainerEnvEntries() throws IOException {
        List<String> lines = Files.readAllLines(MICROSERVICE_CFN_TEMPLATE, StandardCharsets.UTF_8);
        return lines.stream()
                .map(CONTAINER_ENV_ENTRY_LINE_PATTERN::matcher)
                .filter(Matcher::matches)
                .map(matcher -> new ContainerEnvEntry(
                        Integer.parseInt(matcher.group(1)),
                        matcher.group(2),
                        matcher.group(3)))
                .toList();
    }

    private Map<Integer, ContainerEnvEntry> indexByNumber(List<ContainerEnvEntry> entries) {
        Map<Integer, ContainerEnvEntry> map = new LinkedHashMap<>();
        entries.forEach(entry -> map.put(entry.number(), entry));
        return map;
    }
}
