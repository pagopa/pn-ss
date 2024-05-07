package it.pagopa.pnss.configurationproperties.retry;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "sm.retry.strategy")
public record StateMachineRetryStrategyProperties(Long maxAttempts, Long minBackoff) {
}

