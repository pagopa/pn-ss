package it.pagopa.pnss.configurationproperties.retry;


import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "pn.ss.sqs.event-handler.retry.strategy")
public record SqsEventHandlerRetryStrategyProperties(Long maxAttempts, Long minBackoff) {
}
