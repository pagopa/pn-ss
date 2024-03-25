package it.pagopa.pn.library.sign.configuration;
import com.namirial.sign.library.service.PnSignServiceImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PnSignServiceConfiguration {

    @Bean
    public PnSignServiceImpl pnSignServiceImpl() {
        return new PnSignServiceImpl();
    }
}