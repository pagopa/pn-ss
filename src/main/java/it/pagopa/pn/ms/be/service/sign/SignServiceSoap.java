package it.pagopa.pn.ms.be.service.sign;

import com.sun.xml.ws.encoding.xml.XMLMessage;
import com.sun.xml.ws.util.ByteArrayDataSource;
import com.sun.xml.ws.wsdl.parser.InaccessibleWSDLException;
import it.pagopa.pn.ms.be.service.sign.dto.GenericFileSignRequestV2;
import it.pagopa.pn.ms.be.service.sign.dto.GenericFileSignReturnV2;
import it.pagopa.pn.ms.be.service.sign.dto.InputPdfFileSignRequestV2;
import it.pagopa.pn.ms.be.service.sign.dto.PdfFileSignReturnV2;
import it.pagopa.pn.ms.be.service.sign.wsdl.*;

import org.springframework.stereotype.Service;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.xml.bind.JAXBException;
import java.io.InputStream;
import java.net.MalformedURLException;

@Service
public class SignServiceSoap extends CommonArubaService {

    protected SignServiceSoap() throws MalformedURLException {
    }

    public SignReturnV2 singnPdfDocument(byte[] pdfFile, Boolean marcatura) throws TypeOfTransportNotImplemented_Exception, JAXBException {
           SignRequestV2 signRequestV2 = new SignRequestV2();
           signRequestV2.setCertID("AS0");
           signRequestV2.setIdentity(createIdentity(null));
           signRequestV2.setRequiredmark(marcatura);
           signRequestV2.setBinaryinput(pdfFile);
           signRequestV2.setTransport(TypeTransport.BYNARYNET);



           logCallAruba(signRequestV2);

           ArubaSignService service = arubaSignService.getArubaSignServicePort();
           SignReturnV2 signReturnV2 = service.pdfsignatureV2(signRequestV2,null ,null,null ,null,null);
           return  signReturnV2;
       }




    public SignReturnV2 pkcs7signV2(byte[] buf, Boolean marcatura) throws TypeOfTransportNotImplemented_Exception, JAXBException {
        SignRequestV2 signRequestV2 = new SignRequestV2();
        signRequestV2.setCertID("AS0");
        signRequestV2.setIdentity(createIdentity(null));
        DataSource source = new ByteArrayDataSource(buf, "application/octet-stream");
        signRequestV2.setStream(new DataHandler(source));
        signRequestV2.setTransport(TypeTransport.STREAM);
        signRequestV2.setRequiredmark(marcatura);

        logCallAruba(signRequestV2);

        ArubaSignService service = arubaSignService.getArubaSignServicePort();
        SignReturnV2 signReturnV2 = service.pkcs7SignV2(signRequestV2,false,true);
        return  signReturnV2;
    }

    public SignReturnV2 xmlsignature(String contentType, InputStream xml, Boolean marcatura) throws TypeOfTransportNotImplemented_Exception, JAXBException {
        SignRequestV2 signRequestV2 = new SignRequestV2();
        signRequestV2.setCertID("AS0");
        signRequestV2.setIdentity(createIdentity(null));
        DataSource dataSourceXml = XMLMessage.createDataSource( contentType,xml);
        signRequestV2.setStream(new DataHandler(dataSourceXml));
        signRequestV2.setTransport(TypeTransport.STREAM);
        signRequestV2.setRequiredmark(marcatura);


        logCallAruba(signRequestV2);


        ArubaSignService service = arubaSignService.getArubaSignServicePort();
        XmlSignatureParameter parameter = new XmlSignatureParameter();
        parameter.setType(XmlSignatureType.XMLENVELOPED);
        SignReturnV2 signReturnV2 = service.xmlsignature(signRequestV2,parameter );
        signReturnV2.getStream().getContentType();
        return  signReturnV2;
    }


    public PdfFileSignReturnV2 callArubaSignPdfFile(InputPdfFileSignRequestV2 input) throws JAXBException, TypeOfTransportNotImplemented_Exception {
        logCallAruba(input.getInfoTosigned());

        ArubaSignService service = arubaSignService.getArubaSignServicePort();
        SignReturnV2 signReturnV2 = service.pdfsignatureV2(input.getInfoTosigned(),null ,null,null ,null,null);
        PdfFileSignReturnV2 response = new PdfFileSignReturnV2();
        response.setPdfInfoResultSign(signReturnV2);
        return  response;
    }
    public GenericFileSignReturnV2 callGenericFile(GenericFileSignRequestV2 input) throws JAXBException, TypeOfTransportNotImplemented_Exception {
        logCallAruba(input.getInfoTosigned());
        GenericFileSignReturnV2 response = new GenericFileSignReturnV2();

        try {
            arubaSignService = createArubaService(input.getUrl());
            ArubaSignService service = arubaSignService.getArubaSignServicePort();
            SignReturnV2 signReturnV2 = service.pkcs7SignV2(input.getInfoTosigned(),false,true);
            response.setPdfInfoResultSign(signReturnV2);
        } catch (MalformedURLException e) {
            e.printStackTrace();
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
