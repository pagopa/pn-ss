package it.pagopa.pnss.configurationproperties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "pn.ss.task.execution.pool")
public record TaskExecutorConfigurationProperties(Integer maxSize, Integer coreSize, Integer queueCapacity) {
}
