package it.pagopa.pnss.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.Semaphore;


@Configuration
public class SemaphoreConfiguration {

    @Value("${transformation-service-max-thread-pool-size}")
    private Integer maxThreadPoolSize;


    @Bean
    public Semaphore semaphore() {
        return new Semaphore(maxThreadPoolSize);
    }

}
