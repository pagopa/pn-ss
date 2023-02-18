package it.pagopa.pnss.transformation.rest;

import com.amazonaws.services.sqs.AmazonSQSAsync;
import io.awspring.cloud.messaging.core.QueueMessagingTemplate;
import it.pagopa.pn.template.internal.rest.v1.dto.Document;
import it.pagopa.pnss.configurationproperties.QueueName;
import it.pagopa.pnss.transformation.model.S3ObjectCreated;
import it.pagopa.pnss.transformation.service.SignServiceSoap;
import it.pagopa.pnss.transformation.wsdl.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import javax.xml.bind.JAXBException;
import java.io.*;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static it.pagopa.pnss.common.QueueNameConstant.SIGN_QUEUE_NAME;

@RestController
@RequestMapping("/aruba")
public class ArubaController {


    @Autowired
    SignServiceSoap signServiceSoap;

    @Autowired
    AmazonSQSAsync amazonSQSAsync;
    @Autowired
    QueueMessagingTemplate queueMessagingTemplate;

    @Autowired
    QueueName queName;

    @GetMapping(path = "/pdfsignatureV2", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity <SignReturnV2> pdfsignatureV2(
            @RequestParam(name ="marcatura") Boolean marcatura
    ) throws TypeOfTransportNotImplemented_Exception, JAXBException, MalformedURLException {

        byte[] pdfDocument = readPdfDocoument();


        SignReturnV2 response = signServiceSoap.singnPdfDocument(pdfDocument,marcatura);

        return ResponseEntity.ok()
                .body(response);
    }

    @PostMapping(path = "/insertMessageQue", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity <Void> insertMessageQue(
            @RequestBody(required = false) S3ObjectCreated s3obj
    ) throws TypeOfTransportNotImplemented_Exception, JAXBException, MalformedURLException {
        amazonSQSAsync.listQueues();
        amazonSQSAsync.listQueuesAsync();
        queueMessagingTemplate.convertAndSend(queName.signQueueName(),s3obj);

        return ResponseEntity.ok(null);
                //.body(response);
    }

    @GetMapping(path = "/xmlsignature", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity <byte[]> xmlsignature(
            @RequestParam(name ="marcatura") Boolean marcatura) throws TypeOfTransportNotImplemented_Exception, IOException, JAXBException {


        InputStream targetStream =  getClass().getResourceAsStream("/prova.xml");

        SignReturnV2 response = signServiceSoap.xmlsignature(    "application/xml",targetStream,marcatura);

        return ResponseEntity.ok()
                .body(response.getStream().getDataSource().getInputStream().readAllBytes());
    }

    @GetMapping(path = "/pkcs7signV2", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity <byte[]>  pkcs7signV2( @RequestParam(name ="marcatura") Boolean marcatura
                ) throws TypeOfTransportNotImplemented_Exception, IOException, JAXBException {

        byte[] pdfDocument = readPdfDocoument();
        SignReturnV2 response = signServiceSoap.pkcs7signV2(pdfDocument,marcatura);
        response.getStream().getDataSource().getOutputStream();
        return ResponseEntity.ok()
                .body(response.getStream().getDataSource().getInputStream().readAllBytes());
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
