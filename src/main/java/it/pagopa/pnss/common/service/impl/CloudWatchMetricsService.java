package it.pagopa.pnss.common.service.impl;

import it.pagopa.pn.library.sign.pojo.PnSignDocumentResponse;
import it.pagopa.pnss.common.exception.CloudWatchResourceNotFoundException;
import it.pagopa.pnss.configuration.cloudwatch.CloudWatchMetricPublisherConfiguration;
import it.pagopa.pnss.configuration.cloudwatch.MetricsDimensionConfiguration;
import lombok.CustomLog;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.metrics.MetricCollector;
import software.amazon.awssdk.metrics.SdkMetric;
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient;

import java.util.stream.Stream;

import static it.pagopa.pn.library.sign.pojo.SignatureType.*;
import static it.pagopa.pnss.common.utils.LogUtils.*;

/**
 * A service class to publish CloudWatch metrics. It can directly publish metrics to CloudWatch or make use of the CloudWatchMetricPublisherConfiguration class.
 *
 * @see CloudWatchMetricPublisherConfiguration
 */
@Service
@CustomLog
public class CloudWatchMetricsService {

    private final CloudWatchMetricPublisherConfiguration cloudWatchMetricPublisherConfiguration;
    private final MetricsDimensionConfiguration metricsDimensionConfiguration;
    private final CloudWatchAsyncClient cloudWatchAsyncClient;
    @Value("${pn.sign.cloudwatch.metric.dimension.file-size-range}")
    private String fileSizeRangeDimensionName;
    @Value("${pn.sign.cloudwatch.metric.response-time.pades}")
    private String signPadesReadResponseTimeMetric;
    @Value("${pn.sign.cloudwatch.metric.response-time.xades}")
    private String signXadesReadResponseTimeMetric;
    @Value("${pn.sign.cloudwatch.metric.response-time.cades}")
    private String signCadesReadResponseTimeMetric;

    /**
     * Constructor for the CloudWatchMetricsService class.
     *
     * @param cloudWatchMetricPublisherConfiguration Configuration for the CloudWatchMetricPublisher.
     * @param metricsDimensionConfiguration          Configuration for the metrics dimensions.
     * @param cloudWatchAsyncClient                  The CloudWatchAsyncClient to use for publishing metrics.
     */
    public CloudWatchMetricsService(CloudWatchMetricPublisherConfiguration cloudWatchMetricPublisherConfiguration, MetricsDimensionConfiguration metricsDimensionConfiguration, CloudWatchAsyncClient cloudWatchAsyncClient) {
        this.cloudWatchMetricPublisherConfiguration = cloudWatchMetricPublisherConfiguration;
        this.metricsDimensionConfiguration = metricsDimensionConfiguration;
        this.cloudWatchAsyncClient = cloudWatchAsyncClient;
    }

    /**
     * Method to execute a Mono of any type and publish its execution time to CloudWatch
     *
     * @param mono       the mono to execute
     * @param namespace  the metric namespace
     * @param metricName the metric name
     * @return the mono
     */
    public Mono<PnSignDocumentResponse> executeAndPublishResponseTime(Mono<PnSignDocumentResponse> mono, String namespace, String metricName) {
        return mono.elapsed()
                .flatMap(tuple -> publishResponseTime(namespace, metricName, tuple.getT1(), tuple.getT2().getSignedDocument().length / 1024)
                        .thenReturn(tuple.getT2()));
    }

    /**
     * Method to publish a response time related CloudWatch metric, using the CloudWatchMetricPublisherConfiguration class.
     * In order not to block the chain with an error, the method emits onComplete() even if an error occurred while publishing the metric.
     *
     * @param namespace   the metric namespace
     * @param signType    the sign type
     * @param elapsedTime the response time
     * @param fileSize    the file size
     * @return a void Mono
     */
    public Mono<Void> publishResponseTime(String namespace, String signType, long elapsedTime, long fileSize) {
        return Mono.fromRunnable(() -> {
                    log.debug(CLIENT_METHOD_INVOCATION, PUBLISH_RESPONSE_TIME, Stream.of(namespace, signType, elapsedTime, fileSize).toList());
                    String metricName = getMetricNameBySignType(signType);
                    SdkMetric<Long> responseTimeMetric = (SdkMetric<Long>) cloudWatchMetricPublisherConfiguration.getSdkMetricByName(metricName);
                    SdkMetric<String> fileSizeDimension = (SdkMetric<String>) cloudWatchMetricPublisherConfiguration.getSdkMetricByName(fileSizeRangeDimensionName);

                    MetricCollector metricCollector = MetricCollector.create(metricName);
                    //The response time related metric
                    metricCollector.reportMetric(responseTimeMetric, elapsedTime);
                    //The file size related dimension
                    metricCollector.reportMetric(fileSizeDimension, metricsDimensionConfiguration.getDimension(signType, fileSize).value());

                    cloudWatchMetricPublisherConfiguration.getMetricPublisherByNamespace(namespace).publish(metricCollector.collect());
                })
                .onErrorResume(throwable -> {
                    //This exception is thrown when the given file size is not included in a defined range inside the metrics dimension schema.
                    //So no dimension can be defined for the given file size.
                    if (throwable instanceof CloudWatchResourceNotFoundException.DimensionNotFound) {
                        log.warn(throwable.getMessage(), throwable);
                    } else log.error(EXCEPTION_IN_PROCESS, PUBLISH_RESPONSE_TIME, throwable, throwable.getMessage());
                    return Mono.empty();
                }).then();
    }

    /**
     * Method to get the metric name by sign type.
     *
     * @param signType the sign type
     * @return the metric name
     */
    private String getMetricNameBySignType(String signType) {
        return switch (signType) {
            case CADES -> signCadesReadResponseTimeMetric;
            case PADES -> signPadesReadResponseTimeMetric;
            case XADES -> signXadesReadResponseTimeMetric;
            default -> throw new IllegalArgumentException("Invalid sign type");
        };
    }

}