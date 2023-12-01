package it.pagopa.pn.library.sign.configuration;

import it.pagopa.pnss.transformation.wsdl.ArubaSignService;
import it.pagopa.pnss.transformation.wsdl.ArubaSignServiceService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;

@Configuration
public class ArubaSignServiceConf {

    @Bean
    public ArubaSignService arubaSignService() throws IOException {
        return new ArubaSignServiceService(new ClassPathResource("wsdl/ArubaSignService.wsdl").getURL()).getArubaSignServicePort();
    }
}
