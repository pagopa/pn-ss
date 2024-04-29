package it.pagopa.pn.library.sign.configuration;

import it.pagopa.pn.library.sign.exception.CloudWatchResourceNotFoundException;
import lombok.CustomLog;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.metrics.SdkMetric;
import software.amazon.awssdk.metrics.publishers.cloudwatch.CloudWatchMetricPublisher;
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient;

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
    private final Map<String, CloudWatchMetricPublisher> cloudWatchMetricPublishers = new HashMap<>();

    public CloudWatchMetricPublisher getMetricPublisherByNamespace(String namespace) {
        try {
            return cloudWatchMetricPublishers.get(namespace);
        } catch (NullPointerException e) {
            throw new CloudWatchResourceNotFoundException.MetricPublisherNotFoundException(namespace);
        }
    }


}