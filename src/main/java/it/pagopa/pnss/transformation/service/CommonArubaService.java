package it.pagopa.pnss.transformation.service;

import it.pagopa.pnss.transformation.wsdl.ArubaSignServiceService;
import it.pagopa.pnss.transformation.wsdl.Auth;
import it.pagopa.pnss.transformation.wsdl.SignRequestV2;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.namespace.QName;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

public abstract class CommonArubaService {

    public static String ARUBA_RESP_OK = "OK";

    ArubaSignServiceService arubaSignService = createArubaService(null);


    Auth identity;
    @Value("${aruba.delegated.domain}")
    public   String delegated_domain;
    @Value("${aruba.delegated.password}")
    public   String delegated_password;
    @Value("${aruba.delegated.user}")
    public   String delegated_user;
    @Value("${aruba.otpPwd}")
    public   String otpPwd;
    @Value("${aruba.typeOtpAuth}")
    public   String typeOtpAuth;
    @Value("${aruba.user}")
    public   String user;

    @Value("${aruba.cert_id}")
    public String certId = "AS0";
    @Value("${aruba.sign.wsdl.url}")
    public   String arubaUrlWsdl;

    @Value("${aruba.enabled.log}")
    public   Boolean enableArubaLog;


    protected CommonArubaService() throws MalformedURLException {
    }

    public Auth createIdentity (Auth auth){
        if (auth==null ){
            auth = new Auth();
            auth.setDelegatedDomain(delegated_domain);
            auth.setDelegatedPassword(delegated_password);
            auth.setDelegatedUser(delegated_user);
            auth.setOtpPwd(otpPwd);
            auth.setTypeOtpAuth(typeOtpAuth);
            auth.setUser(user);
        }

        return  auth;

    }

    public ArubaSignServiceService createArubaService (String url ) throws MalformedURLException {
        if (StringUtils.isEmpty(url)){
            url = "https://arss.demo.firma-automatica.it:443/ArubaSignService/ArubaSignService";
        }
        URL newEndpoint = new URL(url);
        QName qname = new QName("http://arubasignservice.arubapec.it/","ArubaSignServiceService");
        return new ArubaSignServiceService(newEndpoint, qname);

    }


    public void logCallAruba(SignRequestV2 signRequestV2) throws JAXBException {
        JAXBContext jaxbContext = JAXBContext.newInstance(SignRequestV2.class);
        Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
        jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        JAXBElement <SignRequestV2> jaxbElement
                = new JAXBElement <SignRequestV2>(new QName("", "SignRequest"), SignRequestV2.class, signRequestV2);
        if (enableArubaLog){
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
