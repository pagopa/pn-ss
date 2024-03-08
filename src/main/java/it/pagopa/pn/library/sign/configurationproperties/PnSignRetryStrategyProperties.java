package it.pagopa.pn.library.sign.configurationproperties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "pn.sign.retry.strategy")
public record PnSignRetryStrategyProperties(Long maxAttempts, Long minBackoff) {
}
