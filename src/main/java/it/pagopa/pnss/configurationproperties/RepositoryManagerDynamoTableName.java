package it.pagopa.pnss.configurationproperties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "dynamo.table.repository-manager")
public record RepositoryManagerDynamoTableName( String anagraficaClientName, String tipologieDocumenti,
                                               String documenti) {}
