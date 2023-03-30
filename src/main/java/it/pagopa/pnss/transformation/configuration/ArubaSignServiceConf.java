package it.pagopa.pnss.transformation.configuration;

import it.pagopa.pnss.transformation.wsdl.ArubaSignService;
import it.pagopa.pnss.transformation.wsdl.ArubaSignServiceService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.net.URL;

@Configuration
public class ArubaSignServiceConf {

    @Value("${aruba.sign.wsdl.url}")
    private String arubaUrlWsdl;

    @Bean
    public ArubaSignService arubaSignService() throws IOException {
        return new ArubaSignServiceService(new URL(arubaUrlWsdl)).getArubaSignServicePort();
    }
}
