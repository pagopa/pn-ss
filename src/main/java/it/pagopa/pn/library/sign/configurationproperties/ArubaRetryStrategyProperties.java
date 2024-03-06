package it.pagopa.pn.library.sign.configurationproperties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "aruba.retry.strategy")
public record ArubaRetryStrategyProperties(Long maxAttempts, Long minBackoff) {
}
