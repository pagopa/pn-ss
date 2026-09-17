package it.pagopa.pnss.configuration.cloudwatch;

import it.pagopa.pnss.common.exception.CloudWatchResourceNotFoundException;
import it.pagopa.pnss.configurationproperties.PnSsConfig;
import it.pagopa.pnss.testutils.annotation.SpringBootTestWebEnv;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@SpringBootTestWebEnv
class CloudWatchMetricPublisherConfigurationTest {

    @Autowired
    private CloudWatchMetricPublisherConfiguration config;
    @Autowired
    private PnSsConfig pnSsConfig;

    @Test
    void getMetricPublisherByNamespaceOk() {
        Assertions.assertDoesNotThrow(() -> config.getMetricPublisherByNamespace(pnSsConfig.getSign().getCloudwatch().getNamespaceNamirial()));
    }

    @Test
    void getMetricPublisherByNamespaceNotFound() {
        Assertions.assertThrows(CloudWatchResourceNotFoundException.MetricPublisherNotFoundException.class,
                () -> config.getMetricPublisherByNamespace("nonExistentNamespace"));
    }

    @Test
    void getSdkMetricByNameOk() {
        Assertions.assertDoesNotThrow(() -> config.getSdkMetricByName(pnSsConfig.getSign().getCloudwatch().getMetricResponseTime().getPades()));
    }

    @Test
    void getSdkMetricByNameNotFound() {
        Assertions.assertThrows(CloudWatchResourceNotFoundException.SdkMetricNotFoundException.class,
                () -> config.getSdkMetricByName("nonExistentSdkMetric"));
    }

}
