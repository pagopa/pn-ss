package it.pagopa.pnss.configuration.scheduling;


import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@ConditionalOnProperty( value = "app.scheduling.enable",
                        havingValue = "true",
                        matchIfMissing = true)
@Configuration
@EnableScheduling
public class SchedulingConfiguration {
}
