package it.pagopa.pn.library.sign.configuration;

import com.namirial.sign.library.service.PnSignServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.util.Set;

@Configuration
public class PnSignServiceConfiguration {

    private final Set<String> namirialPropertiesKeySet = Set.of(
            "pn.ss.sign.namirial.address",
            "pn.ss.sign.namirial.max-connections",
            "pn.ss.sign.namirial.pending-acquire-timeout");


    @Bean
    public PnSignServiceImpl pnSignServiceImpl(@Autowired Environment env) {
        namirialPropertiesKeySet.forEach(key -> {
            String property = env.getRequiredProperty(key);
            System.setProperty(toNamirialSystemPropertyKey(key), property);
        });
        return new PnSignServiceImpl();
    }

    private static String toNamirialSystemPropertyKey(String springPropertyKey) {
        return "namirial.server." + springPropertyKey.substring(springPropertyKey.lastIndexOf('.') + 1);
    }
}