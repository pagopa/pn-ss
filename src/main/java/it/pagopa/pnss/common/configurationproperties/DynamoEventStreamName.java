package it.pagopa.pnss.common.configurationproperties;

import org.springframework.boot.context.properties.ConfigurationProperties;

    @ConfigurationProperties(prefix = "dynamo.event.stream")
    public record DynamoEventStreamName(String documentName, String tableMetadata) {}

