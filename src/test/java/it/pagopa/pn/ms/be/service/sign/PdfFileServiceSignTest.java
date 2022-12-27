package it.pagopa.pn.ms.be.service.sign;

import com.sun.xml.ws.util.ByteArrayDataSource;
import it.pagopa.pn.ms.be.service.sign.dto.InputPdfFileSignRequestV2;
import it.pagopa.pn.ms.be.service.sign.dto.PdfFileSignReturnV2;
import it.pagopa.pn.ms.be.service.sign.wsdl.SignRequestV2;
import it.pagopa.pn.ms.be.service.sign.wsdl.TypeOfTransportNotImplemented_Exception;
import it.pagopa.pn.ms.be.service.sign.wsdl.TypeTransport;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.xml.bind.JAXBException;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;


@ExtendWith(SpringExtension.class)
@SpringBootTest
public class PdfFileServiceSignTest {

    @Autowired
    SignServiceSoap service;



    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testPDFFileSignedAndMarked() throws JAXBException, TypeOfTransportNotImplemented_Exception {
        InputPdfFileSignRequestV2 input = new InputPdfFileSignRequestV2();
        byte[] buf = readPdfDocoument();
        SignRequestV2 signRequestV2 = new SignRequestV2();
        signRequestV2.setCertID("AS0");
        signRequestV2.setIdentity(service.createIdentity(null));
        DataSource source = new ByteArrayDataSource(buf, "application/octet-stream");
        signRequestV2.setStream(new DataHandler(source));
        signRequestV2.setTransport(TypeTransport.STREAM);
        signRequestV2.setRequiredmark(false);

        input.setInfoTosigned(signRequestV2);


        PdfFileSignReturnV2 reponse = service.callArubaSignPdfFile(input);
        Assertions.assertNotNull(reponse);
    }
    @Test
    public void testPDFFileCorrupted() throws JAXBException, TypeOfTransportNotImplemented_Exception {
        InputPdfFileSignRequestV2 input = new InputPdfFileSignRequestV2();
        byte[] buf = readPdfDocoument();
        SignRequestV2 signRequestV2 = new SignRequestV2();
        signRequestV2.setCertID("AS0");
        signRequestV2.setIdentity(service.createIdentity(null));
        DataSource source = new ByteArrayDataSource(buf, "application/octet-stream");
        signRequestV2.setStream(new DataHandler(source));
        signRequestV2.setTransport(TypeTransport.STREAM);
        signRequestV2.setRequiredmark(false);

        input.setInfoTosigned(signRequestV2);


        PdfFileSignReturnV2 reponse = service.callArubaSignPdfFile(input);
        Assertions.assertNotNull(reponse);
        Assertions.assertEquals(reponse.getCode(),"0002");
        Assertions.assertEquals(reponse.getDescription(),"STREAM EMPTY" );

    }
    @Test
    public void testPDFFileTranspotType() throws JAXBException, TypeOfTransportNotImplemented_Exception {
        InputPdfFileSignRequestV2 input = new InputPdfFileSignRequestV2();
        byte[] buf = readPdfDocoument();
        SignRequestV2 signRequestV2 = new SignRequestV2();
        signRequestV2.setCertID("AS0");
        signRequestV2.setIdentity(service.createIdentity(null));
        DataSource source = new ByteArrayDataSource(buf, "application/octet-stream");
        signRequestV2.setStream(new DataHandler(source));
        signRequestV2.setTransport(TypeTransport.STREAM);
        signRequestV2.setRequiredmark(false);

        input.setInfoTosigned(signRequestV2);


        PdfFileSignReturnV2 reponse = service.callArubaSignPdfFile(input);
        Assertions.assertNotNull(reponse);
        Assertions.assertEquals(reponse.getCode(),"0002");
        Assertions.assertEquals(reponse.getDescription(),"Incorrect parameters for the type of transport indicated" );
    }
    @Test
    public void testPDFFileLoginKO() throws JAXBException, TypeOfTransportNotImplemented_Exception {
        InputPdfFileSignRequestV2 input = new InputPdfFileSignRequestV2();
        byte[] buf = readPdfDocoument();
        SignRequestV2 signRequestV2 = new SignRequestV2();
        signRequestV2.setCertID("AS0");
        signRequestV2.setIdentity(service.createIdentity(null));
        DataSource source = new ByteArrayDataSource(buf, "application/octet-stream");
        signRequestV2.setStream(new DataHandler(source));
        signRequestV2.setTransport(TypeTransport.STREAM);
        signRequestV2.setRequiredmark(false);

        input.setInfoTosigned(signRequestV2);


        PdfFileSignReturnV2 reponse = service.callArubaSignPdfFile(input);
        Assertions.assertNotNull(reponse);

        Assertions.assertEquals(reponse.getCode(),"0003");
        Assertions.assertEquals(reponse.getDescription(),"Error during the credential verification phase" );
    }
    @Test
    public void testPDFFileTranspotTypeEmpty() throws JAXBException, TypeOfTransportNotImplemented_Exception {
        InputPdfFileSignRequestV2 input = new InputPdfFileSignRequestV2();
        byte[] buf = readPdfDocoument();
        SignRequestV2 signRequestV2 = new SignRequestV2();
        signRequestV2.setCertID("AS0");
        signRequestV2.setIdentity(service.createIdentity(null));
        DataSource source = new ByteArrayDataSource(buf, "application/octet-stream");
        signRequestV2.setStream(new DataHandler(source));
        signRequestV2.setTransport(TypeTransport.STREAM);
        signRequestV2.setRequiredmark(false);

        input.setInfoTosigned(signRequestV2);


        PdfFileSignReturnV2 reponse = service.callArubaSignPdfFile(input);
        Assertions.assertNotNull(reponse);
        Assertions.assertEquals(reponse.getCode(),"0005");
        Assertions.assertEquals(reponse.getDescription(),"Invalid type of transport" );
    }
    @Test
    public void testPDFFileUrlIncorrect() throws JAXBException, TypeOfTransportNotImplemented_Exception {
        InputPdfFileSignRequestV2 input = new InputPdfFileSignRequestV2();
        byte[] buf = readPdfDocoument();
        SignRequestV2 signRequestV2 = new SignRequestV2();
        signRequestV2.setCertID("AS0");
        signRequestV2.setIdentity(service.createIdentity(null));
        DataSource source = new ByteArrayDataSource(buf, "application/octet-stream");
        signRequestV2.setStream(new DataHandler(source));
        signRequestV2.setTransport(TypeTransport.STREAM);
        signRequestV2.setRequiredmark(false);

        input.setInfoTosigned(signRequestV2);


        PdfFileSignReturnV2 reponse = service.callArubaSignPdfFile(input);
        Assertions.assertNotNull(reponse);
        Assertions.assertEquals(reponse.getCode(),"500");
        Assertions.assertEquals(reponse.getDescription(),"Connection refused" );
    }
    @Test
    public void testPDFFileCertIdNotcorrect() throws JAXBException, TypeOfTransportNotImplemented_Exception {
        InputPdfFileSignRequestV2 input = new InputPdfFileSignRequestV2();
        byte[] buf = readPdfDocoument();
        SignRequestV2 signRequestV2 = new SignRequestV2();
        signRequestV2.setCertID("AS0");
        signRequestV2.setIdentity(service.createIdentity(null));
        DataSource source = new ByteArrayDataSource(buf, "application/octet-stream");
        signRequestV2.setStream(new DataHandler(source));
        signRequestV2.setTransport(TypeTransport.STREAM);
        signRequestV2.setRequiredmark(false);

        input.setInfoTosigned(signRequestV2);


        PdfFileSignReturnV2 reponse = service.callArubaSignPdfFile(input);
        Assertions.assertNotNull(reponse);
        Assertions.assertEquals(reponse.getCode(),"0001");
        Assertions.assertEquals(reponse.getDescription(),"PDF error in the signature process" );
    }



    @Test
    public void testPDFFileSignedNotMarked() throws JAXBException, TypeOfTransportNotImplemented_Exception {
        InputPdfFileSignRequestV2 input = new InputPdfFileSignRequestV2();
        byte[] buf = readPdfDocoument();
        SignRequestV2 signRequestV2 = new SignRequestV2();
        signRequestV2.setCertID("AS0");
        signRequestV2.setIdentity(service.createIdentity(null));
        DataSource source = new ByteArrayDataSource(buf, "application/octet-stream");
        signRequestV2.setStream(new DataHandler(source));
        signRequestV2.setTransport(TypeTransport.STREAM);
        signRequestV2.setRequiredmark(false);

        input.setInfoTosigned(signRequestV2);


        PdfFileSignReturnV2 reponse = service.callArubaSignPdfFile(input);
        Assertions.assertNotNull(reponse);
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
