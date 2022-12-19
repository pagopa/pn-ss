package it.pagopa.pn.ms.be.service.sign;

import com.sun.xml.ws.encoding.xml.XMLMessage;
import com.sun.xml.ws.util.ByteArrayDataSource;
import it.pagopa.pn.ms.be.service.sign.wsdl.*;

import org.springframework.stereotype.Service;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.namespace.QName;
import java.io.File;
import java.io.InputStream;

@Service
public class SignServiceStub extends CommonArubaService {

       public SignReturnV2 singnPdfDocument(byte[] pdfFile, Boolean marcatura) throws TypeOfTransportNotImplemented_Exception, JAXBException {
           SignRequestV2 signRequestV2 = new SignRequestV2();
           signRequestV2.setCertID("AS0");
           signRequestV2.setIdentity(createIdentity());
           signRequestV2.setRequiredmark(marcatura);
           signRequestV2.setBinaryinput(pdfFile);
           signRequestV2.setTransport(TypeTransport.BYNARYNET);

           JAXBContext jaxbContext = JAXBContext.newInstance(SignRequestV2.class);
           Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
           jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);

           File file = new File("C:\\PROGETTI\\DGSPA\\workspace\\pn-ssfile.xml");
           JAXBElement <SignRequestV2> jaxbElement
                   = new JAXBElement<SignRequestV2>( new QName("", "SignRequest"), SignRequestV2.class, signRequestV2 );
           jaxbMarshaller.marshal(jaxbElement, System.out);
           jaxbMarshaller.marshal(jaxbElement, file);

           ArubaSignService service = arubaSignService.getArubaSignServicePort();
           SignReturnV2 signReturnV2 = service.pdfsignatureV2(signRequestV2,null ,null,null ,null,null);
           return  signReturnV2;
       }

    public SignReturnV2 xmlsignature(String contentType, InputStream xml, Boolean marcatura) throws TypeOfTransportNotImplemented_Exception, JAXBException {
        SignRequestV2 signRequestV2 = new SignRequestV2();
        signRequestV2.setCertID("AS0");
        signRequestV2.setIdentity(createIdentity());
        DataSource dataSourceXml = XMLMessage.createDataSource( contentType,xml);
        signRequestV2.setStream(new DataHandler(dataSourceXml));
        signRequestV2.setTransport(TypeTransport.STREAM);
        signRequestV2.setRequiredmark(marcatura);

        JAXBContext jaxbContext = JAXBContext.newInstance(SignRequestV2.class);
        Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
        jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);

        File file = new File("C:\\PROGETTI\\DGSPA\\workspace\\pn-ssfile.xml");
        JAXBElement <SignRequestV2> jaxbElement
                = new JAXBElement<SignRequestV2>( new QName("", "SignRequest"), SignRequestV2.class, signRequestV2 );
        jaxbMarshaller.marshal(jaxbElement, System.out);
        jaxbMarshaller.marshal(jaxbElement, file);


        ArubaSignService service = arubaSignService.getArubaSignServicePort();
        XmlSignatureParameter parameter = new XmlSignatureParameter();
        parameter.setType(XmlSignatureType.XMLENVELOPED);
        SignReturnV2 signReturnV2 = service.xmlsignature(signRequestV2,parameter );
        signReturnV2.getStream().getContentType();
        return  signReturnV2;
    }

    public SignReturnV2 pkcs7signV2(byte[] buf, Boolean marcatura) throws TypeOfTransportNotImplemented_Exception, JAXBException {
        SignRequestV2 signRequestV2 = new SignRequestV2();
        signRequestV2.setCertID("AS0");
        signRequestV2.setIdentity(createIdentity());
        DataSource source = new ByteArrayDataSource(buf, "application/octet-stream");

        signRequestV2.setStream(new DataHandler(source));
        signRequestV2.setTransport(TypeTransport.STREAM);

        signRequestV2.setRequiredmark(marcatura);


        JAXBContext jaxbContext = JAXBContext.newInstance(SignRequestV2.class);
        Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
        jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);

        File file = new File("C:\\PROGETTI\\DGSPA\\workspace\\pn-ssfile.xml");
        JAXBElement <SignRequestV2> jaxbElement
                = new JAXBElement<SignRequestV2>( new QName("", "SignRequest"), SignRequestV2.class, signRequestV2 );
        jaxbMarshaller.marshal(jaxbElement, System.out);
        jaxbMarshaller.marshal(jaxbElement, file);

        ArubaSignService service = arubaSignService.getArubaSignServicePort();
        SignReturnV2 signReturnV2 = service.pkcs7SignV2(signRequestV2,false,true);
        return  signReturnV2;
    }
}
