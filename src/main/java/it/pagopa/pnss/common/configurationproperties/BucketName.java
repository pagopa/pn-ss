package it.pagopa.pnss.common.configurationproperties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "s3.bucket")
public record BucketName(
		String ssHotName,
		String ssStageName) {

}
