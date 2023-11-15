package it.pagopa.pnss.configurationproperties.retry;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "s3.retry.strategy")
public record S3RetryStrategyProperties(Long maxAttempts, Long minBackoff) {
}
