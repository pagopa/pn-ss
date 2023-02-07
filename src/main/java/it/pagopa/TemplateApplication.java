package it.pagopa;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.PropertySource;

@SpringBootApplication
@ConfigurationPropertiesScan

//<-- REPOSITORY MANAGER -->
//DYNAMO TABLES
@PropertySource("classpath:repositorymanager/repository-manager-dynamo-table.properties")

public class TemplateApplication {

    public static void main(String[] args) {
        SpringApplication.run(TemplateApplication.class, args);
    }


}