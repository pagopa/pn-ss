package it.pagopa.pn.ms.be.service.sign;

import com.sun.xml.ws.util.ByteArrayDataSource;
import it.pagopa.pn.ms.be.service.sign.dto.GenericFileSignRequestV2;
import it.pagopa.pn.ms.be.service.sign.dto.GenericFileSignReturnV2;
import it.pagopa.pn.ms.be.service.sign.wsdl.ArubaSignServiceService;
import it.pagopa.pn.ms.be.service.sign.wsdl.SignRequestV2;
import it.pagopa.pn.ms.be.service.sign.wsdl.TypeOfTransportNotImplemented_Exception;
import it.pagopa.pn.ms.be.service.sign.wsdl.TypeTransport;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;
import javax.xml.ws.BindingProvider;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;

@ExtendWith(MockitoExtension.class)
public class GenericFileServiceSignTest {


    @InjectMocks
    SignServiceSoap service;




    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testGenericFileSignedAndMarked() throws JAXBException, TypeOfTransportNotImplemented_Exception {
        byte[] buf = readPdfDocoument();
        SignRequestV2 signRequestV2 = new SignRequestV2();
        signRequestV2.setCertID("AS0");
        signRequestV2.setIdentity(service.createIdentity(null));
        DataSource source = new ByteArrayDataSource(buf, "application/octet-stream");
        signRequestV2.setStream(new DataHandler(source));
        signRequestV2.setTransport(TypeTransport.STREAM);
        signRequestV2.setRequiredmark(false);


        GenericFileSignRequestV2 input = new GenericFileSignRequestV2();
        input.setInfoTosigned(signRequestV2);
        GenericFileSignReturnV2 response = service.callGenericFile(input);
        Assertions.assertNotNull(response);
    }
    @Test
    public void testGenericFileCorrupted() throws JAXBException, TypeOfTransportNotImplemented_Exception {
        byte[] buf = readPdfDocoument();
        SignRequestV2 signRequestV2 = new SignRequestV2();
        signRequestV2.setCertID("AS0");
        signRequestV2.setIdentity(service.createIdentity(null));
        DataSource source = new ByteArrayDataSource(buf, "application/octet-stream");
        signRequestV2.setStream(null);
        signRequestV2.setTransport(TypeTransport.STREAM);
        signRequestV2.setRequiredmark(false);


        GenericFileSignRequestV2 input = new GenericFileSignRequestV2();
        input.setInfoTosigned(signRequestV2);
        GenericFileSignReturnV2 response = service.callGenericFile(input);
        Assertions.assertNotNull(response.getPdfInfoResultSign());
        Assertions.assertEquals(response.getPdfInfoResultSign().getReturnCode(),"0002");
        Assertions.assertEquals(response.getPdfInfoResultSign().getDescription(),"STREAM EMPTY" );

    }
    @Test
    public void testGenericFileTranspotType() throws JAXBException, TypeOfTransportNotImplemented_Exception {
        byte[] buf = readPdfDocoument();
        SignRequestV2 signRequestV2 = new SignRequestV2();
        signRequestV2.setCertID("AS0");
        signRequestV2.setIdentity(service.createIdentity(null));
        DataSource source = new ByteArrayDataSource(buf, "application/octet-stream");
        signRequestV2.setStream(new DataHandler(source));
        signRequestV2.setTransport(null);
        signRequestV2.setRequiredmark(false);


        GenericFileSignRequestV2 input = new GenericFileSignRequestV2();
        input.setInfoTosigned(signRequestV2);
        GenericFileSignReturnV2 response = service.callGenericFile(input);
        Assertions.assertNotNull(response.getPdfInfoResultSign());
        Assertions.assertEquals(response.getPdfInfoResultSign().getReturnCode(),"0005");
        Assertions.assertEquals(response.getPdfInfoResultSign().getDescription(),"Trasport Method not Valid");

    }
    @Test
    public void testGenericFileLoginKO() throws JAXBException, TypeOfTransportNotImplemented_Exception {
        byte[] buf = readPdfDocoument();
        SignRequestV2 signRequestV2 = new SignRequestV2();
        signRequestV2.setCertID("AS0");
        signRequestV2.setIdentity(service.createIdentity(null));
        signRequestV2.getIdentity().setUser("");
        DataSource source = new ByteArrayDataSource(buf, "application/octet-stream");
        signRequestV2.setStream(new DataHandler(source));
        signRequestV2.setTransport(TypeTransport.STREAM);
        signRequestV2.setRequiredmark(false);


        GenericFileSignRequestV2 input = new GenericFileSignRequestV2();
        input.setInfoTosigned(signRequestV2);
        GenericFileSignReturnV2 response = service.callGenericFile(input);
        Assertions.assertNotNull(response);

        Assertions.assertEquals(response.getPdfInfoResultSign().getReturnCode(),"0003");
        Assertions.assertEquals(response.getPdfInfoResultSign().getDescription(),"Invalid User Credentials" );
    }
    @Test
    public void testGenericFileTranspotTypeEmpty() throws JAXBException, TypeOfTransportNotImplemented_Exception {
        byte[] buf = readPdfDocoument();
        SignRequestV2 signRequestV2 = new SignRequestV2();
        signRequestV2.setCertID("AS0");
        signRequestV2.setIdentity(service.createIdentity(null));
        DataSource source = new ByteArrayDataSource(buf, "application/octet-stream");
        signRequestV2.setStream(new DataHandler(source));
        signRequestV2.setTransport(TypeTransport.BYNARYNET);
        signRequestV2.setRequiredmark(false);


        GenericFileSignRequestV2 input = new GenericFileSignRequestV2();
        input.setInfoTosigned(signRequestV2);
        GenericFileSignReturnV2 response = service.callGenericFile(input);
        Assertions.assertNotNull(response);
        Assertions.assertEquals(response.getPdfInfoResultSign().getReturnCode(),"0002");
        Assertions.assertEquals(response.getPdfInfoResultSign().getDescription(),"BYNARY INPUT EMPTY" );
    }
    @Test
    public void testGenericFileUrlIncorrect() throws JAXBException, TypeOfTransportNotImplemented_Exception, MalformedURLException {
        byte[] buf = readPdfDocoument();
        SignRequestV2 signRequestV2 = new SignRequestV2();
        signRequestV2.setCertID("AS0");
        signRequestV2.setIdentity(service.createIdentity(null));
        DataSource source = new ByteArrayDataSource(buf, "application/octet-stream");
        signRequestV2.setStream(new DataHandler(source));
        signRequestV2.setTransport(TypeTransport.STREAM);
        signRequestV2.setRequiredmark(false);


        GenericFileSignRequestV2 input = new GenericFileSignRequestV2();
        input.setInfoTosigned(signRequestV2);
        input.setUrl("http://localhost:8080");

        GenericFileSignReturnV2 response = service.callGenericFile(input);
        Assertions.assertNotNull(response);
        Assertions.assertEquals(response.getCode(),"500");
        Assertions.assertEquals(response.getDescription(),"Connection refused: connect" );
    }
    @Test
    public void testGenericFileCertIdNotcorrect() throws JAXBException, TypeOfTransportNotImplemented_Exception {
        byte[] buf = readPdfDocoument();
        SignRequestV2 signRequestV2 = new SignRequestV2();
        signRequestV2.setCertID("");
        signRequestV2.setIdentity(service.createIdentity(null));
        DataSource source = new ByteArrayDataSource(buf, "application/octet-stream");
        signRequestV2.setStream(new DataHandler(source));
        signRequestV2.setTransport(TypeTransport.STREAM);
        signRequestV2.setRequiredmark(false);


        GenericFileSignRequestV2 input = new GenericFileSignRequestV2();
        input.setInfoTosigned(signRequestV2);
        GenericFileSignReturnV2 response = service.callGenericFile(input);
        Assertions.assertNotNull(response);
        Assertions.assertEquals(response.getPdfInfoResultSign().getReturnCode(),"0001");
        Assertions.assertEquals(response.getPdfInfoResultSign().getDescription(),"Generic error" );
    }
    @Test
    public void testGenericFileSignedNotMarked() throws JAXBException, TypeOfTransportNotImplemented_Exception {
        byte[] buf = readPdfDocoument();
        SignRequestV2 signRequestV2 = new SignRequestV2();
        signRequestV2.setCertID("AS0");
        signRequestV2.setIdentity(service.createIdentity(null));
        DataSource source = new ByteArrayDataSource(buf, "application/octet-stream");
        signRequestV2.setStream(new DataHandler(source));
        signRequestV2.setTransport(TypeTransport.STREAM);
        signRequestV2.setRequiredmark(false);


        GenericFileSignRequestV2 input = new GenericFileSignRequestV2();
        input.setInfoTosigned(signRequestV2);
        GenericFileSignReturnV2 response = service.callGenericFile(input);
        Assertions.assertNotNull(response);

    }


    private byte[] readPdfDocoument() {
        byte[] byteArray=null;
        try {

            String filePath = "C:///PROGETTI//DGSPA//materiale start//FirmaAutomatica.pdf";

            byteArray = Files.readAllBytes(Paths.get(filePath));

        } catch (FileNotFoundException e) {
            System.out.println("File Not found"+e);
        } catch (IOException e) {
            System.out.println("IO Ex"+e);
        }
        return byteArray;

    }


}
