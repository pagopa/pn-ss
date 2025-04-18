package it.pagopa.pnss.configuration.cloudwatch;

import it.pagopa.pnss.common.exception.CloudWatchResourceNotFoundException;
import lombok.CustomLog;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.metrics.MetricCategory;
import software.amazon.awssdk.metrics.MetricLevel;
import software.amazon.awssdk.metrics.SdkMetric;
import software.amazon.awssdk.metrics.publishers.cloudwatch.CloudWatchMetricPublisher;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * A configuration class for publishing CloudWatch metrics. It prepares objects to handle asynchronous and optimized publishing of CloudWatch metrics.
 * This configuration class makes use of the AWS CloudWatchMetricPublisher system (Refer to <a href="https://sdk.amazonaws.com/java/api/latest/software/amazon/awssdk/metrics/publishers/cloudwatch/CloudWatchMetricPublisher.html">CloudWatchMetricPublisher</a>)
 */
@Configuration
@CustomLog
public class CloudWatchMetricPublisherConfiguration {
    @Value("${pn.sign.cloudwatch.namespace.aruba}")
    private String arubaPecNamespace;
    @Value("${pn.sign.cloudwatch.namespace.namirial}")
    private String namirialPecNamespace;
    @Value("${pn.sign.cloudwatch.metric.response-time.pades}")
    private String signPadesReadResponseTimeMetric;
    @Value("${pn.sign.cloudwatch.metric.response-time.xades}")
    private String signXadesReadResponseTimeMetric;
    @Value("${pn.sign.cloudwatch.metric.response-time.cades}")
    private String signCadesReadResponseTimeMetric;
    @Value("${pn.sign.cloudwatch.metric.dimension.file-size-range}")
    private String fileSizeRangeDimensionName;
    @Value("${pn.sign.cloudwatch.publisher.maximum-calls-per-upload:#{null}}")
    private int maximumCallsPerUpload;
    @Value("${pn.sign.cloudwatch.publisher.upload-frequency-millis:#{null}}")
    private int uploadFrequencyMillis;
    private final Map<String, CloudWatchMetricPublisher> cloudWatchMetricPublishers = new HashMap<>();
    private final Map<String, SdkMetric<?>> sdkMetrics = new HashMap<>();

    /**
     * Init method to initialize MetricPublishers and SdkMetrics
     */
    @PostConstruct
    private void init() {
        log.debug("Initializing CloudWatchMetricPublisher configurations.");
        initCloudWatchMetricPublishers();
    }

    @PreDestroy
    private void destroy() {
        cloudWatchMetricPublishers.values().forEach(CloudWatchMetricPublisher::close);
    }

    /**
     * Gets metric publisher by namespace.
     *
     * @param namespace the namespace
     * @return the metric publisher by namespace
     */
    public CloudWatchMetricPublisher getMetricPublisherByNamespace(String namespace) {
        CloudWatchMetricPublisher publisher = cloudWatchMetricPublishers.get(namespace);
        if (publisher == null) {
            throw new CloudWatchResourceNotFoundException.MetricPublisherNotFoundException(namespace);
        }
        return publisher;
    }

    /**
     * Gets sdk metric by name.
     *
     * @param metricName the metric name
     * @return the sdk metric by name
     */
    public SdkMetric<?> getSdkMetricByName(String metricName) {
        SdkMetric<?> sdkMetric = sdkMetrics.get(metricName);
        if (sdkMetric == null) {
            throw new CloudWatchResourceNotFoundException.SdkMetricNotFoundException(metricName);
        }
        return sdkMetric;
    }

    /**
     * Initializes the CloudWatchMetricPublishers for the different namespaces
     */
    private void initCloudWatchMetricPublishers() {
        initSkdMetrics();
        CloudWatchMetricPublisher.Builder cloudWatchMetricPublisherBuilder = CloudWatchMetricPublisher.builder()
                .dimensions((SdkMetric<String>) sdkMetrics.get(fileSizeRangeDimensionName))
                .maximumCallsPerUpload(maximumCallsPerUpload)
                .uploadFrequency(Duration.ofMillis(uploadFrequencyMillis));
        cloudWatchMetricPublishers.put(arubaPecNamespace, cloudWatchMetricPublisherBuilder.namespace(arubaPecNamespace).build());
        cloudWatchMetricPublishers.put(namirialPecNamespace, cloudWatchMetricPublisherBuilder.namespace(namirialPecNamespace).build());
    }

    /**
     * Initializes the SdkMetrics for the different metrics
     */
    private void initSkdMetrics() {
        // Creating the SdkMetric representing the file size range dimension
        sdkMetrics.put(fileSizeRangeDimensionName, SdkMetric.create(fileSizeRangeDimensionName, String.class, MetricLevel.INFO, MetricCategory.ALL));
        // Creating SdkMetrics representing the response time metrics for the different sign types
        sdkMetrics.put(signCadesReadResponseTimeMetric, SdkMetric.create(signCadesReadResponseTimeMetric, Long.class, MetricLevel.INFO, MetricCategory.ALL));
        sdkMetrics.put(signXadesReadResponseTimeMetric, SdkMetric.create(signXadesReadResponseTimeMetric, Long.class, MetricLevel.INFO, MetricCategory.ALL));
        sdkMetrics.put(signPadesReadResponseTimeMetric, SdkMetric.create(signPadesReadResponseTimeMetric, Long.class, MetricLevel.INFO, MetricCategory.ALL));
    }


}