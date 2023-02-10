package it.pagopa;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.PropertySource;

@SpringBootApplication
@ConfigurationPropertiesScan

//<-- COMMONS -->
//AWS CONFIGURATION
@PropertySource("classpath:commons/aws-configuration.properties")

//<-- REPOSITORY MANAGER -->
//DYNAMO TABLES
@PropertySource("classpath:repositorymanager/repository-manager-dynamo-table.properties")

// BUCKET
@PropertySource("classpath:bucket/bucket.properties")

// QUEUE
@PropertySource("classpath:queue/queue.properties")

public class TemplateApplication {

    public static void main(String[] args) {
        SpringApplication.run(TemplateApplication.class, args);
    }

}