package it.pagopa.pnss.transformation.service;

import java.net.MalformedURLException;
import java.net.URL;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.namespace.QName;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import it.pagopa.pnss.transformation.model.pojo.ArubaSecretValue;
import it.pagopa.pnss.transformation.wsdl.ArubaSignServiceService;
import it.pagopa.pnss.transformation.wsdl.Auth;
import it.pagopa.pnss.transformation.wsdl.SignRequestV2;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class CommonArubaService {

    public static final String ARUBA_RESP_OK = "OK";

    ArubaSignServiceService arubaSignService = createArubaService(null);

    @Autowired
    private ArubaSecretValue arubaSecretValue;


    Auth identity;
    @Value("${aruba.cert_id}")
    public String certId = "AS0";
    @Value("${aruba.sign.wsdl.url}")
    public String arubaUrlWsdl;

    @Value("${aruba.enabled.log}")
    public Boolean enableArubaLog;


    protected CommonArubaService() throws MalformedURLException {
    }

    public Auth createIdentity(Auth auth) {
    	log.info("CommonArubaService.createIdentity() : START");
        if (auth == null) {
            auth = new Auth();
            auth.setDelegatedDomain(arubaSecretValue.getDelegatedDomain());
            auth.setDelegatedPassword(arubaSecretValue.getDelegatedPassword());
            auth.setDelegatedUser(arubaSecretValue.getDelegatedUser());
            auth.setOtpPwd(arubaSecretValue.getOtpPwd());
            auth.setTypeOtpAuth(arubaSecretValue.getTypeOtpAuth());
            auth.setUser(arubaSecretValue.getUser());
        }

        return auth;

    }

    public ArubaSignServiceService createArubaService(String url) throws MalformedURLException {
    	log.info("CommonArubaService.createArubaService() : START");
    	log.info("CommonArubaService.createArubaService() : url = {}", url);
    	
        if (StringUtils.isEmpty(url)) {
            url = "https://arss.demo.firma-automatica.it:443/ArubaSignService/ArubaSignService";
        }
        URL newEndpoint = new URL(url);
        QName qname = new QName("http://arubasignservice.arubapec.it/", "ArubaSignServiceService");
        return new ArubaSignServiceService(newEndpoint, qname);

    }


    public void logCallAruba(SignRequestV2 signRequestV2) throws JAXBException {
    	log.info("CommonArubaService.logCallAruba() : START");
    	
        JAXBContext jaxbContext = JAXBContext.newInstance(SignRequestV2.class);
        Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
        jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        JAXBElement<SignRequestV2> jaxbElement
                = new JAXBElement<SignRequestV2>(new QName("", "SignRequest"), SignRequestV2.class, signRequestV2);
        if (enableArubaLog) {
            //File file = new File("C:\\PROGETTI\\DGSPA\\workspace\\pn-ssfile.xml");

            jaxbMarshaller.marshal(jaxbElement, System.out);
//        jaxbMarshaller.marshal(jaxbElement, file);
            //jaxbMarshaller.marshal(jaxbElement, file);

        }
    }

    public ArubaSignServiceService getArubaSignService() {
        return arubaSignService;
    }

    public void setArubaSignService(ArubaSignServiceService arubaSignService) {
        this.arubaSignService = arubaSignService;
    }
}
