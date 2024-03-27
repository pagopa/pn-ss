package it.pagopa.pnss.configurationproperties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "gestore.repository.retry.strategy")
public record GestoreRepositoryRetryStrategyProperties(Long maxAttempts, Long minBackoff) {
}
