package it.pagopa;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
public class TemplateApplication {


    public static void main(String[] args) {
        SpringApplication.run(TemplateApplication.class, args);
    }


}