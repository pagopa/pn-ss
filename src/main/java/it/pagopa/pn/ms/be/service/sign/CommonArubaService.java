package it.pagopa.pn.ms.be.service.sign;

import it.pagopa.pn.ms.be.service.sign.wsdl.ArubaSignServiceService;
import it.pagopa.pn.ms.be.service.sign.wsdl.Auth;
import it.pagopa.pn.ms.be.service.sign.wsdl.SignRequestV2;
import org.apache.commons.lang3.StringUtils;

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

    public static  String delegated_domain="demoprod";
    public static  String delegated_password="password11";
    public static  String delegated_user="delegato";
    public static  String otpPwd="dsign";
    public static  String typeOtpAuth="demoprod";
    public static  String user="titolare_aut";

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
        File file = new File("C:\\PROGETTI\\DGSPA\\workspace\\pn-ssfile.xml");
        JAXBElement <SignRequestV2> jaxbElement
                = new JAXBElement <SignRequestV2>(new QName("", "SignRequest"), SignRequestV2.class, signRequestV2);
        jaxbMarshaller.marshal(jaxbElement, System.out);
        jaxbMarshaller.marshal(jaxbElement, file);
    }

    public ArubaSignServiceService getArubaSignService() {
        return arubaSignService;
    }

    public void setArubaSignService(ArubaSignServiceService arubaSignService) {
        this.arubaSignService = arubaSignService;
    }
}
