package it.pagopa.pnss.transformation.configuration;

import it.pagopa.pnss.transformation.wsdl.ArubaSignService;
import it.pagopa.pnss.transformation.wsdl.ArubaSignServiceService;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class ArubaSignServiceConf {
    @Value("${aruba.server.address}")
    private String arubaServerAddress;
    @Bean
    public ArubaSignService arubaSignService() {

            JaxWsProxyFactoryBean factory = new JaxWsProxyFactoryBean();
            factory.setServiceClass(ArubaSignService.class);
            factory.setAddress(arubaServerAddress);
            factory.setWsdlLocation(ArubaSignServiceService.WSDL_LOCATION.getPath());
            factory.setEndpointName(ArubaSignServiceService.ArubaSignServicePort);
            factory.setServiceName(ArubaSignServiceService.SERVICE);
            return factory.create(ArubaSignService.class);
        }
}
