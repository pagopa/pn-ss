package it.pagopa.pnss.configuration.cloudwatch;

import it.pagopa.pnss.common.exception.CloudWatchResourceNotFoundException;
import it.pagopa.pnss.testutils.annotation.SpringBootTestWebEnv;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

@SpringBootTestWebEnv
class CloudWatchMetricPublisherConfigurationTest {

    @Autowired
    private CloudWatchMetricPublisherConfiguration config;
    @Value("${pn.sign.cloudwatch.namespace.namirial}")
    private String namirialNamespace;
    @Value("${pn.sign.cloudwatch.metric.response-time.pades}")
    private String signPadesReadResponseTimeMetric;

    @Test
    void getMetricPublisherByNamespaceOk() {
        Assertions.assertDoesNotThrow(() -> config.getMetricPublisherByNamespace(namirialNamespace));
    }

    @Test
    void getMetricPublisherByNamespaceNotFound() {
        Assertions.assertThrows(CloudWatchResourceNotFoundException.MetricPublisherNotFoundException.class,
                () -> config.getMetricPublisherByNamespace("nonExistentNamespace"));
    }

    @Test
    void getSdkMetricByNameOk() {
        Assertions.assertDoesNotThrow(() -> config.getSdkMetricByName(signPadesReadResponseTimeMetric));
    }

    @Test
    void getSdkMetricByNameNotFound() {
        Assertions.assertThrows(CloudWatchResourceNotFoundException.SdkMetricNotFoundException.class,
                () -> config.getSdkMetricByName("nonExistentSdkMetric"));
    }

}
