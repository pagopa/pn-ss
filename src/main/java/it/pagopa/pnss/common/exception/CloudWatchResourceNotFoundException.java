package it.pagopa.pnss.common.exception;

public class CloudWatchResourceNotFoundException extends RuntimeException {

    public CloudWatchResourceNotFoundException() {
        super("CloudWatch resource not found.");
    }

    public CloudWatchResourceNotFoundException(String message) {
        super(message);
    }

    public static class MetricPublisherNotFoundException extends CloudWatchResourceNotFoundException {
        public MetricPublisherNotFoundException(String namespace) {
            super(String.format("MetricPublisher with namespace '%s' has not been found.", namespace));
        }
    }

    public static class SdkMetricNotFoundException extends CloudWatchResourceNotFoundException {
        public SdkMetricNotFoundException(String metricName) {
            super(String.format("SdkMetric with name '%s' has not been found.", metricName));
        }
    }

    public static class DimensionNotFound extends CloudWatchResourceNotFoundException {
        public DimensionNotFound(String signType, Long fileSize) {
            super(String.format("Dimension not found for signType: %s, fileSize: %s", signType, fileSize));
        }
    }

}
