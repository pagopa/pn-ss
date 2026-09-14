package it.pagopa.pnss.configurationproperties;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = PnSsConfig.class, webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = "spring.config.location=classpath:/application.properties")
@EnableConfigurationProperties(PnSsConfig.class)
class PnSsConfigBindingTest {

    @Autowired
    private PnSsConfig pnSsConfig;

    @Test
    void bucket_shouldBindFromNormalizedPnSsProperties() {
        assertThat(pnSsConfig.getBucket()).isNotNull();
        assertThat(pnSsConfig.getBucket().getHotName()).isEqualTo("PnSsBucketName");
        assertThat(pnSsConfig.getBucket().getStageName()).isEqualTo("PnSsStagingBucketName");
    }

    @Test
    void dynamoRepositoryManager_shouldBindFromNormalizedPnSsProperties() {
        assertThat(pnSsConfig.getDynamo()).isNotNull();
        assertThat(pnSsConfig.getDynamo().getRepositoryManager()).isNotNull();
        assertThat(pnSsConfig.getDynamo().getRepositoryManager().getAnagraficaClientName()).isEqualTo("pn-SsAnagraficaClient");
        assertThat(pnSsConfig.getDynamo().getRepositoryManager().getTipologieDocumentiName()).isEqualTo("pn-SsTipologieDocumenti");
        assertThat(pnSsConfig.getDynamo().getRepositoryManager().getDocumentiName()).isEqualTo("pn-SsDocumenti");
        assertThat(pnSsConfig.getDynamo().getRepositoryManager().getScadenzaDocumentiName()).isEqualTo("pn-SsScadenzaDocumenti");
        assertThat(pnSsConfig.getDynamo().getRepositoryManager().getTagsName()).isEqualTo("pn-SsTags");
    }

    @Test
    void dynamoEventStream_shouldBindFromNormalizedPnSsProperties() {
        assertThat(pnSsConfig.getDynamo()).isNotNull();
        assertThat(pnSsConfig.getDynamo().getEventStream()).isNotNull();
        assertThat(pnSsConfig.getDynamo().getEventStream().getDocumentName()).isEqualTo("PnSsTableDocumentiStreamArn");
        assertThat(pnSsConfig.getDynamo().getEventStream().getTableMetadata()).isEqualTo("TableDocumentiStreamMetadatiTable");
    }

    @Test
    void dynamoRetryStrategy_shouldBindFromNormalizedPnSsProperties() {
        assertThat(pnSsConfig.getDynamo()).isNotNull();
        assertThat(pnSsConfig.getDynamo().getRetryStrategy()).isNotNull();
        assertThat(pnSsConfig.getDynamo().getRetryStrategy().getMaxAttempts()).isEqualTo(3L);
        assertThat(pnSsConfig.getDynamo().getRetryStrategy().getMinBackoff()).isEqualTo(3L);
    }

    @Test
    void sqsAvailability_shouldBindFromNormalizedPnSsProperties() {
        assertThat(pnSsConfig.getSqs()).isNotNull();
        assertThat(pnSsConfig.getSqs().getAvailability()).isNotNull();
        assertThat(pnSsConfig.getSqs().getAvailability().getSqsName()).isEqualTo("pn-ss-availability-events-queue");
    }

    @Test
    void eventBridge_shouldBindFromNormalizedPnSsProperties() {
        assertThat(pnSsConfig.getEventBridge()).isNotNull();
        assertThat(pnSsConfig.getEventBridge().getDisponibilitaDocumentiName()).isEqualTo("Pn-Ss-Notifications-Bus");
    }

    @Test
    void endpointStateMachine_shouldBindFromNormalizedPnSsProperties() {
        assertThat(pnSsConfig.getEndpoint()).isNotNull();
        assertThat(pnSsConfig.getEndpoint().getStateMachine()).isNotNull();
        assertThat(pnSsConfig.getEndpoint().getStateMachine().getContainerBaseUrl()).isEqualTo("http://localhost:8082");
        assertThat(pnSsConfig.getEndpoint().getStateMachine().getValidate()).isEqualTo("/statemachinemanager/validate/{processId}/{currStatus}");
    }

    @Test
    void gestoreRepositoryRetryStrategy_shouldBindFromNormalizedPnSsProperties() {
        assertThat(pnSsConfig.getGestoreRepository()).isNotNull();
        assertThat(pnSsConfig.getGestoreRepository().getRetryStrategy()).isNotNull();
        assertThat(pnSsConfig.getGestoreRepository().getRetryStrategy().getMaxAttempts()).isEqualTo(3L);
        assertThat(pnSsConfig.getGestoreRepository().getRetryStrategy().getMinBackoff()).isEqualTo(3L);
    }

    @Test
    void s3RetryStrategy_shouldBindFromNormalizedPnSsProperties() {
        assertThat(pnSsConfig.getS3()).isNotNull();
        assertThat(pnSsConfig.getS3().getRetryStrategy()).isNotNull();
        assertThat(pnSsConfig.getS3().getRetryStrategy().getMaxAttempts()).isEqualTo(3L);
        assertThat(pnSsConfig.getS3().getRetryStrategy().getMinBackoff()).isEqualTo(3L);
    }

    @Test
    void clientInterniEndpoint_shouldBindFromNormalizedPnSsProperties() {
        assertThat(pnSsConfig.getClientInterni()).isNotNull();
        assertThat(pnSsConfig.getClientInterni().getEndpoint()).isNotNull();
        assertThat(pnSsConfig.getClientInterni().getEndpoint().getDocClient()).isEqualTo("/safestorage/internal/v1/documents/%s");
        assertThat(pnSsConfig.getClientInterni().getEndpoint().getDocClientPost()).isEqualTo("/safestorage/internal/v1/documents");
        assertThat(pnSsConfig.getClientInterni().getEndpoint().getUserConfiguration()).isEqualTo("/safestorage/internal/v1/userConfigurations/%s");
        assertThat(pnSsConfig.getClientInterni().getEndpoint().getScadenzaDocumentiPost()).isEqualTo("/safestorage/internal/v1/scadenza-documenti");
        assertThat(pnSsConfig.getClientInterni().getEndpoint().getConfigurationApiDocumentsConfig()).isEqualTo("/safe-storage/v1/configurations/documents-types");
        assertThat(pnSsConfig.getClientInterni().getEndpoint().getDocTypes()).isEqualTo("/safestorage/internal/v1/doctypes/%s");
        assertThat(pnSsConfig.getClientInterni().getEndpoint().getTagsGet()).isEqualTo("/safestorage/internal/v1/tags/%s");
        assertThat(pnSsConfig.getClientInterni().getEndpoint().getTagsPut()).isEqualTo("/safestorage/internal/v1/documents/%s/tags");
    }

    @Test
    void clientInterniHeaderAndQueryParamAndBaseUrl_shouldBindFromNormalizedPnSsProperties() {
        assertThat(pnSsConfig.getClientInterni()).isNotNull();
        assertThat(pnSsConfig.getClientInterni().getHeader()).isNotNull();
        assertThat(pnSsConfig.getClientInterni().getHeader().getApiKey()).isEqualTo("x-api-key");
        assertThat(pnSsConfig.getClientInterni().getHeader().getPagopaSafestorageCxId()).isEqualTo("x-pagopa-safestorage-cx-id");
        assertThat(pnSsConfig.getClientInterni().getHeader().getCorrelationId()).isEqualTo("x-pagopa-pn-cx-id");
        assertThat(pnSsConfig.getClientInterni().getQueryParam()).isNotNull();
        assertThat(pnSsConfig.getClientInterni().getQueryParam().getPresignedUrlTraceId()).isEqualTo("x-amzn-trace-id");
        assertThat(pnSsConfig.getClientInterni().getBaseUrl()).isEqualTo("http://localhost:8080");
    }

    @Test
    void clientInterniJetty_shouldBindFromNormalizedPnSsProperties() {
        assertThat(pnSsConfig.getClientInterni()).isNotNull();
        assertThat(pnSsConfig.getClientInterni().getJetty()).isNotNull();
        assertThat(pnSsConfig.getClientInterni().getJetty().getMaxConnectionsPerDestination()).isEqualTo(200);
    }

    @Test
    void signIdentitySignature_shouldBindFromNormalizedPnSsProperties() {
        assertThat(pnSsConfig.getSign()).isNotNull();
        assertThat(pnSsConfig.getSign().getIdentitySignature()).isEqualTo("Pn-SS-SignAndTimemark");
    }

    @Test
    void signCloudwatch_shouldBindFromNormalizedPnSsProperties() {
        assertThat(pnSsConfig.getSign()).isNotNull();
        assertThat(pnSsConfig.getSign().getCloudwatch()).isNotNull();
        assertThat(pnSsConfig.getSign().getCloudwatch().getNamespaceAruba()).isEqualTo("Sign/Aruba");
        assertThat(pnSsConfig.getSign().getCloudwatch().getNamespaceNamirial()).isEqualTo("Sign/Namirial");
        assertThat(pnSsConfig.getSign().getCloudwatch().getDimensionMetricsSchema()).isNotBlank();
        assertThat(pnSsConfig.getSign().getCloudwatch().getMetricDimensionFileSizeRange()).isEqualTo("FileSizeRange");
        assertThat(pnSsConfig.getSign().getCloudwatch().getMetricResponseTime()).isNotNull();
        assertThat(pnSsConfig.getSign().getCloudwatch().getMetricResponseTime().getPades()).isEqualTo("SignPADESDocumentResponseTime");
        assertThat(pnSsConfig.getSign().getCloudwatch().getMetricResponseTime().getCades()).isEqualTo("SignCADESDocumentResponseTime");
        assertThat(pnSsConfig.getSign().getCloudwatch().getMetricResponseTime().getXades()).isEqualTo("SignXADESDocumentResponseTime");
        assertThat(pnSsConfig.getSign().getCloudwatch().getPublisher()).isNotNull();
        assertThat(pnSsConfig.getSign().getCloudwatch().getPublisher().getMaximumCallsPerUpload()).isEqualTo(10);
        assertThat(pnSsConfig.getSign().getCloudwatch().getPublisher().getUploadFrequencyMillis()).isEqualTo(60000L);
    }

    @Test
    void eventHandlerAndSafeClients_shouldBindFromNormalizedPnSsProperties() {
        assertThat(pnSsConfig.getEventHandler()).isNotNull();
        assertThat(pnSsConfig.getEventHandler().getMaxMessages()).isEqualTo(10);
        assertThat(pnSsConfig.getSafeClients()).isNotNull();
    }

    @Test
    void retention_shouldBindFromNormalizedPnSsProperties() {
        assertThat(pnSsConfig.getRetention()).isNotNull();
        assertThat(pnSsConfig.getRetention().getDefaultInternalApiKeyValue()).isEqualTo("internal-api.key");
        assertThat(pnSsConfig.getRetention().getDefaultInternalClientIdValue()).isEqualTo("internal");
        assertThat(pnSsConfig.getRetention().getObjectLockRetentionMode()).isEqualTo("GOVERNANCE");
        assertThat(pnSsConfig.getRetention().getDaysToIgnore()).isEqualTo(7);
    }

    @Test
    void uriBuilder_shouldBindFromNormalizedPnSsProperties() {
        assertThat(pnSsConfig.getUriBuilder()).isNotNull();
        assertThat(pnSsConfig.getUriBuilder().getPresignedUrl()).isNotNull();
        assertThat(pnSsConfig.getUriBuilder().getPresignedUrl().getDurationMinutesUpload()).isEqualTo(60);
        assertThat(pnSsConfig.getUriBuilder().getPresignedUrl().getDurationMinutesDownload()).isEqualTo(60);
        assertThat(pnSsConfig.getUriBuilder().getStayHotBucketTimeDays()).isEqualTo(2);
        assertThat(pnSsConfig.getUriBuilder().getGetFileWithPatchConfiguration()).isEqualTo("NONE");
        assertThat(pnSsConfig.getUriBuilder().getMaxRestoreTimeCold()).isEqualByComparingTo("18000");
        assertThat(pnSsConfig.getUriBuilder().getRestoreRequestDateHeaderName()).isEqualTo("x-amz-restore-request-date");
        assertThat(pnSsConfig.getUriBuilder().getInitialNewDocumentState()).isEqualTo("BOOKED");
    }

    @Test
    void indexing_shouldBindFromNormalizedPnSsProperties() {
        assertThat(pnSsConfig.getIndexing()).isNotNull();
        assertThat(pnSsConfig.getIndexing().getConfigurationName()).isNotBlank();
        assertThat(pnSsConfig.getIndexing().getDocumentNumberOfPagesTagKey()).isEqualTo("document_number_of_pages");
    }

    @Test
    void ignoredUpdateMetadata_shouldBindFromNormalizedPnSsProperties() {
        assertThat(pnSsConfig.getIgnoredUpdateMetadata()).isNotNull();
        assertThat(pnSsConfig.getIgnoredUpdateMetadata().getList()).isNotNull();
    }
}
