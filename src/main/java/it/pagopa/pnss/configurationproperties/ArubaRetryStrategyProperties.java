package it.pagopa.pnss.configurationproperties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "aruba.retry.strategy")
public record ArubaRetryStrategyProperties(Long maxAttempts, Long minBackoff) {
}
