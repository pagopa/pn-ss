package it.pagopa.pnss.common.configurationproperties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "event.bridge")
public record AvailabelDocumentEventBridgeName(String disponibilitaDocumentiName) {}