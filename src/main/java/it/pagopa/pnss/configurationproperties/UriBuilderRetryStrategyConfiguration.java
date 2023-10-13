package it.pagopa.pnss.configurationproperties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "uri.builder.retry.strategy")
public record UriBuilderRetryStrategyConfiguration(Long maxAttempts, Long minBackoff) {
}
