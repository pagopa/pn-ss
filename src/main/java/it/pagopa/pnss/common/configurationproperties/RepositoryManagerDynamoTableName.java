package it.pagopa.pnss.common.configurationproperties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "dynamo.table.repository-manager")
public record RepositoryManagerDynamoTableName(String anagraficaClientName, String tipologieDocumentiName,
                                               String documentiName) {}
