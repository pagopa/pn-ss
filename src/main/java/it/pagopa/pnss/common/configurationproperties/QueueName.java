package it.pagopa.pnss.common.configurationproperties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "s3.queue")
public record QueueName(String signQueueName) {

}
