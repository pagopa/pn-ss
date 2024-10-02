package it.pagopa.pnss.configurationproperties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "sqs.queue")
public record StreamRecordProcessorQueueName(String batchName, String errorName, String dlqErrorName) {
}
