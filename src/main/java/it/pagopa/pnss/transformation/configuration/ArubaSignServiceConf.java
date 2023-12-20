package it.pagopa.pnss.transformation.configuration;

import it.pagopa.pnss.transformation.wsdl.ArubaSignService;
import it.pagopa.pnss.transformation.wsdl.ArubaSignServiceService;
import lombok.CustomLog;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static it.pagopa.pnss.common.utils.LogUtils.INITIALIZING_ARUBA_PROXY_CLIENT;


@Configuration
@CustomLog
public class ArubaSignServiceConf {
    @Value("${aruba.server.address}")
    private String arubaServerAddress;
    @Bean
    public ArubaSignService arubaSignService() {
            var endpointName = ArubaSignServiceService.ArubaSignServicePort;
            var serviceName = ArubaSignServiceService.SERVICE;
            log.debug(INITIALIZING_ARUBA_PROXY_CLIENT, "pn-ss", arubaServerAddress, endpointName, serviceName);
            JaxWsProxyFactoryBean factory = new JaxWsProxyFactoryBean();
            factory.setServiceClass(ArubaSignService.class);
            factory.setAddress(arubaServerAddress);
            factory.setEndpointName(endpointName);
            factory.setServiceName(serviceName);
            return factory.create(ArubaSignService.class);
        }
}
