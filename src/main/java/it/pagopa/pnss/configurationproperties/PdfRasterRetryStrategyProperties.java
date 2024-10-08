package it.pagopa.pnss.configurationproperties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "pdf-raster.retry.strategy")
public record PdfRasterRetryStrategyProperties(Long maxAttempts, Long minBackoff) {
}