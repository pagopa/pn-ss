package it.pagopa.pnss.transformation;

import com.sun.xml.ws.util.ByteArrayDataSource;
import it.pagopa.pnss.transformation.model.InputPdfFileSignRequestV2;
import it.pagopa.pnss.transformation.model.PdfFileSignReturnV2;
import it.pagopa.pnss.transformation.service.SignServiceSoap;
import it.pagopa.pnss.transformation.wsdl.SignRequestV2;
import it.pagopa.pnss.transformation.wsdl.TypeOfTransportNotImplemented_Exception;
import it.pagopa.pnss.transformation.wsdl.TypeTransport;
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

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;


@ExtendWith(MockitoExtension.class)
public class PdfFileServiceSignTest {

    @InjectMocks
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


        PdfFileSignReturnV2 response = service.callArubaSignPdfFile(input);
        Assertions.assertNotNull(response);
    }
    @Test
    public void testPDFFileCorrupted() throws JAXBException, TypeOfTransportNotImplemented_Exception {
        InputPdfFileSignRequestV2 input = new InputPdfFileSignRequestV2();
        byte[] buf = readPdfDocoument();
        SignRequestV2 signRequestV2 = new SignRequestV2();
        signRequestV2.setCertID("AS0");
        signRequestV2.setIdentity(service.createIdentity(null));
        DataSource source = new ByteArrayDataSource(buf, "application/octet-stream");
        signRequestV2.setStream(null);
        signRequestV2.setTransport(TypeTransport.STREAM);
        signRequestV2.setRequiredmark(false);

        input.setInfoTosigned(signRequestV2);


        PdfFileSignReturnV2 response = service.callArubaSignPdfFile(input);
        Assertions.assertNotNull(response);
        Assertions.assertEquals(response.getCode(),"0002");
        Assertions.assertEquals(response.getDescription(),"STREAM EMPTY" );

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
        signRequestV2.setTransport(null);
        signRequestV2.setRequiredmark(false);

        input.setInfoTosigned(signRequestV2);


        PdfFileSignReturnV2 response = service.callArubaSignPdfFile(input);
        Assertions.assertNotNull(response);
        Assertions.assertEquals(response.getCode(),"0005");
        Assertions.assertEquals(response.getDescription(),"Trasport Method not Valid");;
    }
    @Test
    public void testPDFFileLoginKO() throws JAXBException, TypeOfTransportNotImplemented_Exception {
        InputPdfFileSignRequestV2 input = new InputPdfFileSignRequestV2();
        byte[] buf = readPdfDocoument();
        SignRequestV2 signRequestV2 = new SignRequestV2();
        signRequestV2.setCertID("AS0");
        signRequestV2.setIdentity(service.createIdentity(null));
        signRequestV2.getIdentity().setUser("");
        DataSource source = new ByteArrayDataSource(buf, "application/octet-stream");
        signRequestV2.setStream(new DataHandler(source));
        signRequestV2.setTransport(TypeTransport.STREAM);
        signRequestV2.setRequiredmark(false);

        input.setInfoTosigned(signRequestV2);


        PdfFileSignReturnV2 response = service.callArubaSignPdfFile(input);
        Assertions.assertNotNull(response);

        Assertions.assertEquals(response.getCode(),"0003");
        Assertions.assertEquals(response.getDescription(),"Invalid User Credentials" );
    }
    @Test
    public void testPDFFielTranspotTypeEmpty() throws JAXBException, TypeOfTransportNotImplemented_Exception {
        InputPdfFileSignRequestV2 input = new InputPdfFileSignRequestV2();
        byte[] buf = readPdfDocoument();
        SignRequestV2 signRequestV2 = new SignRequestV2();
        signRequestV2.setCertID("AS0");
        signRequestV2.setIdentity(service.createIdentity(null));
        DataSource source = new ByteArrayDataSource(buf, "application/octet-stream");
        signRequestV2.setStream(new DataHandler(source));
        signRequestV2.setTransport(TypeTransport.BYNARYNET);
        signRequestV2.setRequiredmark(false);

        input.setInfoTosigned(signRequestV2);


        PdfFileSignReturnV2 response = service.callArubaSignPdfFile(input);
        Assertions.assertNotNull(response);
        Assertions.assertEquals(response.getCode(),"0002");
        Assertions.assertEquals(response.getDescription(),"BYNARY INPUT EMPTY" );
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
        input.setUrl("http://localhost:8080");

        PdfFileSignReturnV2 response = service.callArubaSignPdfFile(input);
        Assertions.assertNotNull(response);
        Assertions.assertEquals(response.getCode(),"500");
        //Assertions.assertEquals(response.getDescription(),"Connection refused: connect" );
    }
    @Test
    public void testPDFFileCertIdNotcorrect() throws JAXBException, TypeOfTransportNotImplemented_Exception {
        InputPdfFileSignRequestV2 input = new InputPdfFileSignRequestV2();
        byte[] buf = readPdfDocoument();
        SignRequestV2 signRequestV2 = new SignRequestV2();
        signRequestV2.setCertID("");
        signRequestV2.setIdentity(service.createIdentity(null));
        DataSource source = new ByteArrayDataSource(buf, "application/octet-stream");
        signRequestV2.setStream(new DataHandler(source));
        signRequestV2.setTransport(TypeTransport.STREAM);
        signRequestV2.setRequiredmark(false);

        input.setInfoTosigned(signRequestV2);


        PdfFileSignReturnV2 response = service.callArubaSignPdfFile(input);
        Assertions.assertNotNull(response);
        Assertions.assertEquals(response.getCode(),"0001");
        Assertions.assertEquals(response.getDescription(),"Generic error" );
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


        PdfFileSignReturnV2 response = service.callArubaSignPdfFile(input);
        Assertions.assertNotNull(response);
    }



    private byte[] readPdfDocoument() {
        byte[] byteArray=null;
        try {

            InputStream is =  getClass().getResourceAsStream("/PDF_PROVA.pdf");
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();

            int nRead;
            byte[] data = new byte[16384];

            while ((nRead = is.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, nRead);
            }

            byteArray = buffer.toByteArray();

        } catch (FileNotFoundException e) {
            System.out.println("File Not found"+e);
        } catch (IOException e) {
            System.out.println("IO Ex"+e);
        }
        return byteArray;

    }


}
