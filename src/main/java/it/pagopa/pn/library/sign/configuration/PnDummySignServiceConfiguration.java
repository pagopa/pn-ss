package it.pagopa.pn.library.sign.configuration;


import it.pagopa.pn.ss.dummy.sign.service.PnDummySignServiceImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PnDummySignServiceConfiguration {

    @Bean
    public PnDummySignServiceImpl pnDummySignServiceImpl() {
        return new PnDummySignServiceImpl();
    }
}
