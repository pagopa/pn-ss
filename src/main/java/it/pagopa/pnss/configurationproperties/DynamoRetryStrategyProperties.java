package it.pagopa.pnss.configurationproperties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "dynamo.retry.strategy")
public record DynamoRetryStrategyProperties(Long maxAttempts, Long minBackoff) {
}
