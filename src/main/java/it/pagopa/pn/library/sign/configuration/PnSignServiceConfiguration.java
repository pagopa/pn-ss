package it.pagopa.pn.library.sign.configuration;

import com.namirial.sign.library.service.PnSignServiceImpl;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PnSignServiceConfiguration {

    @Bean
    public PnSignServiceImpl pnSignServiceImpl(@Value("${namirial.server.address}") String namirialServerAddress) {
        System.setProperty("namirial.server.address", namirialServerAddress);
        return new PnSignServiceImpl();
    }
}