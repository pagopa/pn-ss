package it.pagopa.pnss.transformation.service;

import java.io.InputStream;
import java.net.MalformedURLException;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.xml.bind.JAXBException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.sun.xml.ws.encoding.xml.XMLMessage;
import com.sun.xml.ws.util.ByteArrayDataSource;
import com.sun.xml.ws.wsdl.parser.InaccessibleWSDLException;

import it.pagopa.pnss.transformation.model.GenericFileSignRequestV2;
import it.pagopa.pnss.transformation.model.GenericFileSignReturnV2;
import it.pagopa.pnss.transformation.model.InputPdfFileSignRequestV2;
import it.pagopa.pnss.transformation.model.pojo.IdentitySecretTimemark;
import it.pagopa.pnss.transformation.wsdl.ArubaSignService;
import it.pagopa.pnss.transformation.wsdl.SignRequestV2;
import it.pagopa.pnss.transformation.wsdl.SignReturnV2;
import it.pagopa.pnss.transformation.wsdl.TsaAuth;
import it.pagopa.pnss.transformation.wsdl.TypeOfTransportNotImplemented_Exception;
import it.pagopa.pnss.transformation.wsdl.TypeTransport;
import it.pagopa.pnss.transformation.wsdl.XmlSignatureParameter;
import it.pagopa.pnss.transformation.wsdl.XmlSignatureType;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class SignServiceSoap extends CommonArubaService {

    @Value("${aruba.cert_id}")
    public String certificationID;

    @Autowired
    private final IdentitySecretTimemark identitySecretTimemark;

    @Value("${PnSsTimemarkUrl:#{null}}")
    public String timemarkUrl;

    @Value("${PnSsTsaIdentity:#{true}}")
    public boolean tsaIdentity;

    private static final String USER_URL = "SignServiceSoap.singnPdfDocument() : userUrl = {}";
    private static final String PASSWORD_URL = "SignServiceSoap.singnPdfDocument() : passwordUrl = {}";
    private static final String TIMEMARK_URL = "SignServiceSoap.singnPdfDocument() : timemarkUrl = {}";
    private static final String TSA_IDENTITY_URL = "SignServiceSoap.singnPdfDocument() : tsaIdentity = {}";

    protected SignServiceSoap(IdentitySecretTimemark identitySecretTimemark) {
        this.identitySecretTimemark = identitySecretTimemark;
    }

    public SignReturnV2 signPdfDocument(byte[] pdfFile, Boolean marcatura) throws TypeOfTransportNotImplemented_Exception, JAXBException, MalformedURLException {
        SignRequestV2 signRequestV2 = new SignRequestV2();
        signRequestV2.setCertID(certificationID);
        signRequestV2.setIdentity(createIdentity(null));
        signRequestV2.setRequiredmark(marcatura);
        signRequestV2.setBinaryinput(pdfFile);
        signRequestV2.setTransport(TypeTransport.BYNARYNET);

        setSignRequestTsaIdentity(signRequestV2, marcatura);

        logCallAruba(signRequestV2);

        log.debug("SignServiceSoap.singnPdfDocument() : arubaUrlWsdl = {}", arubaUrlWsdl);

        ArubaSignService service = createArubaService(arubaUrlWsdl).getArubaSignServicePort();
        return service.pdfsignatureV2(signRequestV2,null ,null,null ,null,null);
    }



    public SignReturnV2 pkcs7signV2(byte[] buf, Boolean marcatura) throws TypeOfTransportNotImplemented_Exception, JAXBException, MalformedURLException {
        SignRequestV2 signRequestV2 = new SignRequestV2();
        signRequestV2.setCertID(certificationID);
        signRequestV2.setIdentity(createIdentity(null));
        DataSource source = new ByteArrayDataSource(buf, "application/octet-stream");
        signRequestV2.setStream(new DataHandler(source));
        signRequestV2.setTransport(TypeTransport.STREAM);
        signRequestV2.setRequiredmark(marcatura);

        setSignRequestTsaIdentity(signRequestV2, marcatura);

        logCallAruba(signRequestV2);

        ArubaSignService service = createArubaService(arubaUrlWsdl).getArubaSignServicePort();
        return service.pkcs7SignV2(signRequestV2,false,true);
    }

    public SignReturnV2 xmlsignature(String contentType, InputStream xml, Boolean marcatura) throws TypeOfTransportNotImplemented_Exception, MalformedURLException {

        SignRequestV2 signRequestV2 = new SignRequestV2();
        signRequestV2.setCertID(certificationID);
        signRequestV2.setIdentity(createIdentity(null));
        DataSource dataSourceXml = XMLMessage.createDataSource( contentType,xml);
        signRequestV2.setStream(new DataHandler(dataSourceXml));
        signRequestV2.setTransport(TypeTransport.STREAM);
        signRequestV2.setRequiredmark(marcatura);


        logCallAruba(signRequestV2);


        ArubaSignService service = createArubaService(arubaUrlWsdl).getArubaSignServicePort();
        XmlSignatureParameter parameter = new XmlSignatureParameter();
        parameter.setType(XmlSignatureType.XMLENVELOPED);
        SignReturnV2 signReturnV2 = service.xmlsignature(signRequestV2,parameter );
        signReturnV2.getStream().getContentType();
        return  signReturnV2;
    }


    public GenericFileSignReturnV2 callArubaSignPdfFile(InputPdfFileSignRequestV2 input) {
    
        logCallAruba(input.getInfoTosigned());

        GenericFileSignReturnV2 response = new GenericFileSignReturnV2();

        try {
            arubaSignService = createArubaService(input.getUrl());
            ArubaSignService service = arubaSignService.getArubaSignServicePort();
            SignReturnV2 signReturnV2 = service.pdfsignatureV2(input.getInfoTosigned(),null ,null,null ,null,null);
            response.setPdfInfoResultSign(signReturnV2);
            if (!signReturnV2.getStatus().equals(ARUBA_RESP_OK)){
                response.setCode(signReturnV2.getReturnCode());
                response.setDescription(signReturnV2.getDescription());
            }
        } catch (InaccessibleWSDLException iwe){
            response.setCode("500");
            response.setDescription(iwe.getErrors().get(0).getMessage());
        } catch (Exception e) {
            response.setCode(e.getCause().getMessage());
            response.setDescription(e.getMessage());
        }


        return  response;
    }
    public GenericFileSignReturnV2 callGenericFile(GenericFileSignRequestV2 input) {
        logCallAruba(input.getInfoTosigned());
        GenericFileSignReturnV2 response = new GenericFileSignReturnV2();

        try {
            arubaSignService = createArubaService(input.getUrl());
            ArubaSignService service = arubaSignService.getArubaSignServicePort();
            SignReturnV2 signReturnV2 = service.pkcs7SignV2(input.getInfoTosigned(),false,true);
            response.setPdfInfoResultSign(signReturnV2);
            if (!signReturnV2.getStatus().equals(ARUBA_RESP_OK)){
                response.setCode(signReturnV2.getReturnCode());
                response.setDescription(signReturnV2.getDescription());
            }
        } catch (InaccessibleWSDLException iwe){
            response.setCode("500");
            response.setDescription(iwe.getErrors().get(0).getMessage());
        } catch (Exception e) {
            response.setCode(e.getCause().getMessage());
            response.setDescription(e.getMessage());
        }
        return response;

    }

    private void setSignRequestTsaIdentity(SignRequestV2 signRequestV2, boolean marcatura)
    {
        if(marcatura) {
            log.debug(TSA_IDENTITY_URL, tsaIdentity);

            if (tsaIdentity) {
                log.debug(USER_URL, identitySecretTimemark.getUserTimemark());
                log.debug(PASSWORD_URL, identitySecretTimemark.getPasswordTimemark());
                log.debug(TIMEMARK_URL, timemarkUrl);

                var tsaAuth = new TsaAuth();
                tsaAuth.setUser(identitySecretTimemark.getUserTimemark());
                tsaAuth.setPassword(identitySecretTimemark.getPasswordTimemark());
                tsaAuth.setTsaurl(timemarkUrl);
                signRequestV2.setTsaIdentity(tsaAuth);
            }
        }
    }



}
