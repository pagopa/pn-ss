package it.pagopa.pnss.common.configurationproperties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "aws")
public record AwsConfigurationProperties(String regionCode) {}
