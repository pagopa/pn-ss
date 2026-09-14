package it.pagopa;

import it.pagopa.pn.commons.configs.listeners.TaskIdApplicationListener;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class TemplateApplication {

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(TemplateApplication.class);
        app.addListeners(new TaskIdApplicationListener());
        app.run(args);
    }

}