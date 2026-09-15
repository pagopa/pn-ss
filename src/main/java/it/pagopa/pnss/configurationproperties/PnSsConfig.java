package it.pagopa.pnss.configurationproperties;

import it.pagopa.pn.commons.conf.SharedAutoConfiguration;
import jakarta.annotation.PostConstruct;
import lombok.CustomLog;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;

@Data
@Configuration
@ConfigurationProperties(prefix = "pn.ss")
@Import(SharedAutoConfiguration.class)
@CustomLog
public class PnSsConfig {

    private Bucket bucket;
    private Dynamo dynamo;
    private Sqs sqs;
    private EventBridge eventBridge;
    private Endpoint endpoint;
    private GestoreRepository gestoreRepository;
    private S3 s3;
    private ClientInterni clientInterni;
    private Sign sign;
    private EventHandler eventHandler;
    private String safeClients;
    private Retention retention;
    private UriBuilder uriBuilder;
    private Indexing indexing;
    private IgnoredUpdateMetadata ignoredUpdateMetadata;

    @PostConstruct
    public void logConfiguration() {
        log.info("PnSsConfig loaded : bucket=[hotName={}, stageName={}]", bucket.getHotName(), bucket.getStageName());
        log.info("PnSsConfig loaded : dynamo.repositoryManager=[anagraficaClientName={}, tipologieDocumentiName={}, documentiName={}, scadenzaDocumentiName={}, tagsName={}]",
                dynamo.getRepositoryManager().getAnagraficaClientName(),
                dynamo.getRepositoryManager().getTipologieDocumentiName(),
                dynamo.getRepositoryManager().getDocumentiName(),
                dynamo.getRepositoryManager().getScadenzaDocumentiName(),
                dynamo.getRepositoryManager().getTagsName());
        log.info("PnSsConfig loaded : dynamo.eventStream=[documentName={}, tableMetadata={}]",
                dynamo.getEventStream().getDocumentName(), dynamo.getEventStream().getTableMetadata());
        log.info("PnSsConfig loaded : dynamo.retryStrategy=[maxAttempts={}, minBackoff={}]",
                dynamo.getRetryStrategy().getMaxAttempts(), dynamo.getRetryStrategy().getMinBackoff());
        log.info("PnSsConfig loaded : sqs.availability=[sqsName={}]", sqs.getAvailability().getSqsName());
        log.info("PnSsConfig loaded : eventBridge=[disponibilitaDocumentiName={}]", eventBridge.getDisponibilitaDocumentiName());
        log.info("PnSsConfig loaded : endpoint.stateMachine=[containerBaseUrl={}, validate={}]",
                endpoint.getStateMachine().getContainerBaseUrl(), endpoint.getStateMachine().getValidate());
        log.info("PnSsConfig loaded : gestoreRepository.retryStrategy=[maxAttempts={}, minBackoff={}]",
                gestoreRepository.getRetryStrategy().getMaxAttempts(), gestoreRepository.getRetryStrategy().getMinBackoff());
        log.info("PnSsConfig loaded : s3.retryStrategy=[maxAttempts={}, minBackoff={}]",
                s3.getRetryStrategy().getMaxAttempts(), s3.getRetryStrategy().getMinBackoff());
    }

    @Data
    public static class Bucket {
        private String hotName;
        private String stageName;
    }

    @Data
    public static class Dynamo {
        private RepositoryManager repositoryManager;
        private EventStream eventStream;
        private RetryStrategy retryStrategy;

        @Data
        public static class RepositoryManager {
            private String anagraficaClientName;
            private String tipologieDocumentiName;
            private String documentiName;
            private String scadenzaDocumentiName;
            private String tagsName;
        }

        @Data
        public static class EventStream {
            private String documentName;
            private String tableMetadata;
        }

        @Data
        public static class RetryStrategy {
            private Long maxAttempts;
            private Long minBackoff;
        }
    }

    @Data
    public static class Sqs {
        private Availability availability;

        @Data
        public static class Availability {
            private String sqsName;
        }
    }

    @Data
    public static class EventBridge {
        private String disponibilitaDocumentiName;
    }

    @Data
    public static class Endpoint {
        private StateMachine stateMachine;

        @Data
        public static class StateMachine {
            private String containerBaseUrl;
            private String validate;
        }
    }

    @Data
    public static class GestoreRepository {
        private RetryStrategy retryStrategy;

        @Data
        public static class RetryStrategy {
            private Long maxAttempts;
            private Long minBackoff;
        }
    }

    @Data
    public static class S3 {
        private RetryStrategy retryStrategy;

        @Data
        public static class RetryStrategy {
            private Long maxAttempts;
            private Long minBackoff;
        }
    }

    @Data
    public static class ClientInterni {
        private Endpoint endpoint;
        private Header header;
        private QueryParam queryParam;
        private String baseUrl;
        private Jetty jetty;

        @Data
        public static class Endpoint {
            private String docClient;
            private String docClientPost;
            private String userConfiguration;
            private String scadenzaDocumentiPost;
            private String configurationApiDocumentsConfig;
            private String docTypes;
            private String tagsGet;
            private String tagsPut;
        }

        @Data
        public static class Header {
            private String apiKey;
            private String pagopaSafestorageCxId;
            private String correlationId;
        }

        @Data
        public static class QueryParam {
            private String presignedUrlTraceId;
        }

        @Data
        public static class Jetty {
            private Integer maxConnectionsPerDestination;
        }
    }

    @Data
    public static class Sign {
        private String identitySignature;
        private CloudWatch cloudwatch;

        @Data
        public static class CloudWatch {
            private String namespaceAruba;
            private String namespaceNamirial;
            private String dimensionMetricsSchema;
            private String metricDimensionFileSizeRange;
            private MetricResponseTime metricResponseTime;
            private Publisher publisher;

            @Data
            public static class MetricResponseTime {
                private String pades;
                private String cades;
                private String xades;
            }

            @Data
            public static class Publisher {
                private Integer maximumCallsPerUpload;
                private Long uploadFrequencyMillis;
            }
        }
    }

    @Data
    public static class EventHandler {
        private Integer maxMessages;
        private String cronStreamsRecordProcessor;
    }

    @Data
    public static class Retention {
        private String defaultInternalApiKeyValue;
        private String defaultInternalClientIdValue;
        private String objectLockRetentionMode;
        private Integer daysToIgnore;
    }

    @Data
    public static class UriBuilder {
        private PresignedUrl presignedUrl;
        private Integer stayHotBucketTimeDays;
        private String getFileWithPatchConfiguration;
        private BigDecimal maxRestoreTimeCold;
        private String restoreRequestDateHeaderName;
        private String initialNewDocumentState;

        @Data
        public static class PresignedUrl {
            private Integer durationMinutesUpload;
            private Integer durationMinutesDownload;
            private String checksumSha256Header;
        }
    }

    @Data
    public static class Indexing {
        private String configurationName;
        private String documentNumberOfPagesTagKey;
    }

    @Data
    public static class IgnoredUpdateMetadata {
        private String list;
    }
}
