package it.pagopa.pnss.transformation.service;

import java.io.IOException;
import java.net.URL;

import javax.xml.namespace.QName;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import it.pagopa.pnss.transformation.wsdl.ArubaSignServiceService;
import lombok.extern.slf4j.Slf4j;

@Configuration
@Slf4j
public class ArubaSignConfiguration {
	
    @Value("${aruba.cert_id}")
    public String certId;
    @Value("${aruba.sign.wsdl.url}")
    public String arubaUrlWsdl;
    @Value("${aruba.enabled.log}")
    public Boolean enableArubaLog;
    @Value("${aruba.qname}")
    public String arubaQname;
    @Value("${aruba.sign.service}")
    public String arubaSignatureService;

    @Bean
    public ArubaSignServiceService getArubaSignServiceService() throws IOException {
    	log.info("getArubaSignServiceService : certId = {}", certId);
    	log.info("getArubaSignServiceService : arubaUrlWsdl = {}", arubaUrlWsdl);
    	log.info("getArubaSignServiceService : arubaQname = {}", arubaQname);
    	log.info("getArubaSignServiceService : arubaSignatureService = {}", arubaSignatureService);
        URL newEndpoint = new URL(arubaUrlWsdl);
        QName qname = new QName(arubaQname, arubaSignatureService);
        return new ArubaSignServiceService(newEndpoint, qname);
    }
    
}
