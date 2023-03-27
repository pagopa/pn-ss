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
import it.pagopa.pnss.transformation.model.PdfFileSignReturnV2;
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

    @Autowired
    private IdentitySecretTimemark identitySecretTimemark;

    @Value("${PnSsTimemarkUrl:#{null}}")
    public String timemarkUrl;

    @Value("${PnSsTsaIdentity:#{true}}")
    public boolean tsaIdentity;

    protected SignServiceSoap(IdentitySecretTimemark identitySecretTimemark) throws MalformedURLException {
        this.identitySecretTimemark = identitySecretTimemark;
    }

    public SignReturnV2 singnPdfDocument(byte[] pdfFile, Boolean marcatura) throws TypeOfTransportNotImplemented_Exception, JAXBException, MalformedURLException {
    	log.info("SignServiceSoap.singnPdfDocument() : START");
    	
        SignRequestV2 signRequestV2 = new SignRequestV2();
        signRequestV2.setCertID(certId);
        signRequestV2.setIdentity(createIdentity(null));
        signRequestV2.setRequiredmark(marcatura);
        signRequestV2.setBinaryinput(pdfFile);
        signRequestV2.setTransport(TypeTransport.BYNARYNET);
        
        log.debug("SignServiceSoap.singnPdfDocument() : userUrl = {}", identitySecretTimemark.getUserTimemark());	
        log.debug("SignServiceSoap.singnPdfDocument() : passwordUrl = {}", identitySecretTimemark.getPasswordTimemark());	
        log.debug("SignServiceSoap.singnPdfDocument() : timemarkUrl = {}", timemarkUrl);

        if(marcatura) {
            log.debug("SignServiceSoap.singnPdfDocument() : tsaIdentity = {}", tsaIdentity);

            if (tsaIdentity) {
                log.debug("SignServiceSoap.singnPdfDocument() : userUrl = {}", identitySecretTimemark.getUserTimemark());
                log.debug("SignServiceSoap.singnPdfDocument() : passwordUrl = {}", identitySecretTimemark.getPasswordTimemark());
                log.debug("SignServiceSoap.singnPdfDocument() : timemarkUrl = {}", timemarkUrl);

                var tsaAuth = new TsaAuth();
                tsaAuth.setUser(identitySecretTimemark.getUserTimemark());
                tsaAuth.setPassword(identitySecretTimemark.getPasswordTimemark());
                tsaAuth.setTsaurl(timemarkUrl);
                signRequestV2.setTsaIdentity(tsaAuth);
            }
        }

        logCallAruba(signRequestV2);
        
        log.debug("SignServiceSoap.singnPdfDocument() : arubaUrlWsdl = {}", arubaUrlWsdl);	

        ArubaSignService service = createArubaService(arubaUrlWsdl).getArubaSignServicePort();
        SignReturnV2 signReturnV2 = service.pdfsignatureV2(signRequestV2,null ,null,null ,null,null);
        return  signReturnV2;
    }



    public SignReturnV2 pkcs7signV2(byte[] buf, Boolean marcatura) throws TypeOfTransportNotImplemented_Exception, JAXBException, MalformedURLException {
    	log.info("SignServiceSoap.pkcs7signV2() : START");
    	
        SignRequestV2 signRequestV2 = new SignRequestV2();
        signRequestV2.setCertID(certId);
        signRequestV2.setIdentity(createIdentity(null));
        DataSource source = new ByteArrayDataSource(buf, "application/octet-stream");
        signRequestV2.setStream(new DataHandler(source));
        signRequestV2.setTransport(TypeTransport.STREAM);
        signRequestV2.setRequiredmark(marcatura);

        if(marcatura) {
            log.debug("SignServiceSoap.singnPdfDocument() : tsaIdentity = {}", tsaIdentity);

            if (tsaIdentity) {
                log.debug("SignServiceSoap.singnPdfDocument() : userUrl = {}", identitySecretTimemark.getUserTimemark());
                log.debug("SignServiceSoap.singnPdfDocument() : passwordUrl = {}", identitySecretTimemark.getPasswordTimemark());
                log.debug("SignServiceSoap.singnPdfDocument() : timemarkUrl = {}", timemarkUrl);

                var tsaAuth = new TsaAuth();
                tsaAuth.setUser(identitySecretTimemark.getUserTimemark());
                tsaAuth.setPassword(identitySecretTimemark.getPasswordTimemark());
                tsaAuth.setTsaurl(timemarkUrl);
                signRequestV2.setTsaIdentity(tsaAuth);
            }
        }

        logCallAruba(signRequestV2);

        ArubaSignService service = createArubaService(arubaUrlWsdl).getArubaSignServicePort();
        SignReturnV2 signReturnV2 = service.pkcs7SignV2(signRequestV2,false,true);
        return  signReturnV2;
    }

    public SignReturnV2 xmlsignature(String contentType, InputStream xml, Boolean marcatura) throws TypeOfTransportNotImplemented_Exception, JAXBException, MalformedURLException {
    	log.info("SignServiceSoap.xmlsignature() : START");
    	
        SignRequestV2 signRequestV2 = new SignRequestV2();
        signRequestV2.setCertID(certId);
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


    public PdfFileSignReturnV2 callArubaSignPdfFile(InputPdfFileSignRequestV2 input) throws JAXBException, TypeOfTransportNotImplemented_Exception {
    	log.info("SignServiceSoap.callArubaSignPdfFile() : START");
    	
        logCallAruba(input.getInfoTosigned());

        PdfFileSignReturnV2 response = new PdfFileSignReturnV2();

        try {
            arubaSignService = createArubaService(input.getUrl());
            ArubaSignService service = arubaSignService.getArubaSignServicePort();
            SignReturnV2 signReturnV2 = service.pdfsignatureV2(input.getInfoTosigned(),null ,null,null ,null,null);
            response.setPdfInfoResultSign(signReturnV2);
            if (!signReturnV2.getStatus().equals(ARUBA_RESP_OK)){
                response.setCode(signReturnV2.getReturnCode());
                response.setDescription(signReturnV2.getDescription());
            }
        } catch (MalformedURLException e) {
            response.setCode(e.getCause().getMessage());
            response.setDescription(e.getMessage());
        }catch (InaccessibleWSDLException iwe){
            iwe.getMessage();
            response.setCode("500");
            response.setDescription(iwe.getErrors().get(0).getMessage());
        }
        catch (Exception ex){
            response.setCode(ex.getCause().getMessage());
            response.setDescription(ex.getMessage());
        }


        return  response;
    }
    public GenericFileSignReturnV2 callGenericFile(GenericFileSignRequestV2 input) throws JAXBException, TypeOfTransportNotImplemented_Exception {
    	log.info("SignServiceSoap.callGenericFile() : START");
    	
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
        } catch (MalformedURLException e) {
            response.setCode(e.getCause().getMessage());
            response.setDescription(e.getMessage());
        }catch (InaccessibleWSDLException iwe){
            iwe.getMessage();
            response.setCode("500");
            response.setDescription(iwe.getErrors().get(0).getMessage());
        }
        catch (Exception ex){
            response.setCode(ex.getCause().getMessage());
            response.setDescription(ex.getMessage());
        }
        return response;

    }



}
