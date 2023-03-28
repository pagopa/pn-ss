package it.pagopa.pnss.transformation.service;

import it.pagopa.pnss.transformation.model.pojo.ArubaSecretValue;
import it.pagopa.pnss.transformation.wsdl.ArubaSignServiceService;
import it.pagopa.pnss.transformation.wsdl.Auth;
import it.pagopa.pnss.transformation.wsdl.SignRequestV2;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.xml.namespace.QName;
import java.net.MalformedURLException;
import java.net.URL;

@Service
@Slf4j
public abstract class CommonArubaService {

    public static String ARUBA_RESP_OK = "OK";

    @Autowired
    ArubaSignServiceService arubaSignService;

    @Autowired
    private ArubaSecretValue arubaSecretValue;


    Auth identity;

    @Value("${aruba.cert_id}")
    public String certId;
    @Value("${aruba.enabled.log}")
    public Boolean enableArubaLog;
    @Value("${aruba.sign.service}")
    public String arubaSignatureService;

    protected CommonArubaService() throws MalformedURLException {
//    	arubaSignService = createArubaService(null);
    }
}
