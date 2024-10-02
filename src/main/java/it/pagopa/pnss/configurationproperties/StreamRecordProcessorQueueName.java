package it.pagopa.pnss.configurationproperties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "sqs.queue.availability")
public record StreamRecordProcessorQueueName(String sqsName) {
}
