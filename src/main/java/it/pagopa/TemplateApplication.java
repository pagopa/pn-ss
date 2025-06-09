package it.pagopa;

import it.pagopa.pn.commons.configs.listeners.TaskIdApplicationListener;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.PropertySource;

@SpringBootApplication
@ConfigurationPropertiesScan

//<-- REPOSITORY MANAGER -->
//DYNAMO TABLES
@PropertySource("classpath:repositorymanager/repository-manager-dynamo-table.properties")

// BUCKET
@PropertySource("classpath:bucket/bucket.properties")

// EVENT STREAM
@PropertySource("classpath:eventStream/dynamo-event-stream.properties")

// EVENT BRIDGE
@PropertySource("classpath:eventBridge/event-bridge-disponibilita-documenti.properties")

// INTERNAL ENDPOINTS
@PropertySource("classpath:commons/internal-endpoint.properties")

// ARUBA
@PropertySource("classpath:sign/aruba/aruba-sign-service.properties")

// PN SIGN
@PropertySource(("classpath:sign/pn-sign.properties"))

// NAMIRIAL
@PropertySource("classpath:sign/namirial.properties")

// IGNORED UPDATE METADATA
@PropertySource("classpath:configuration/ignored-update-metadata.properties")

// INDEXING
@PropertySource("classpath:indexing/indexing.properties")

// TRANSFORMATION
@PropertySource("classpath:transformation/transformation.properties")

// CLOUDWATCH
@PropertySource("classpath:cloudwatch/cloudwatch.properties")

public class TemplateApplication {

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(TemplateApplication.class);
        app.addListeners(new TaskIdApplicationListener());
        app.run(args);
    }

}