package it.pagopa.pnss.configurationproperties.retry;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "pn.ss.sqs.retry.strategy")
public record SqsRetryStrategyProperties(Long maxAttempts, Long minBackoff) {
}
