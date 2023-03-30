package it.pagopa.pnss.transformation.rest;

import com.amazonaws.services.sqs.AmazonSQSAsync;
import io.awspring.cloud.messaging.core.QueueMessagingTemplate;
import it.pagopa.pnss.configurationproperties.BucketName;
import it.pagopa.pnss.configurationproperties.QueueName;
import it.pagopa.pnss.transformation.service.SignServiceSoap;
import it.pagopa.pnss.transformation.wsdl.SignReturnV2;
import it.pagopa.pnss.transformation.wsdl.TypeOfTransportNotImplemented_Exception;
import jakarta.xml.bind.JAXBException;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;


@Data
@RestController
@Slf4j
@RequestMapping("/aruba")
public class ArubaController {

    final SignServiceSoap signServiceSoap;
    final BucketName bucketName;
    final AmazonSQSAsync amazonSQSAsync;
    final QueueMessagingTemplate queueMessagingTemplate;
    final QueueName queName;

    @Value("${test.aws.s3.endpoint:#{null}}")
    String testAwsS3Endpoint;

    public ArubaController(SignServiceSoap signServiceSoap, BucketName bucketName, AmazonSQSAsync amazonSQSAsync,
                           QueueMessagingTemplate queueMessagingTemplate, QueueName queName) {
        this.signServiceSoap = signServiceSoap;
        this.bucketName = bucketName;
        this.amazonSQSAsync = amazonSQSAsync;
        this.queueMessagingTemplate = queueMessagingTemplate;
        this.queName = queName;
    }

    @GetMapping(path = "/pdfsignatureV2", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<SignReturnV2> pdfsignatureV2(@RequestParam(name = "marcatura") Boolean marcatura)
            throws TypeOfTransportNotImplemented_Exception, JAXBException, MalformedURLException {

        byte[] pdfDocument = readPdfDocoument();


        SignReturnV2 response = signServiceSoap.signPdfDocument(pdfDocument, marcatura);

        return ResponseEntity.ok().body(response);
    }

    @GetMapping(path = "/xmlsignature", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<byte[]> xmlsignature(@RequestParam(name = "marcatura") Boolean marcatura) throws IOException, JAXBException {


        InputStream targetStream = getClass().getResourceAsStream("/prova.xml");

        SignReturnV2 response = signServiceSoap.xmlsignature("application/xml", targetStream, marcatura);

        return ResponseEntity.ok().body(response.getStream().getDataSource().getInputStream().readAllBytes());
    }

    @GetMapping(path = "/pkcs7signV2", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<byte[]> pkcs7signV2(@RequestParam(name = "marcatura") Boolean marcatura) throws IOException, JAXBException {

        byte[] pdfDocument = readPdfDocoument();
        SignReturnV2 response = signServiceSoap.pkcs7signV2(pdfDocument, marcatura);
        response.getStream().getDataSource().getOutputStream();
        return ResponseEntity.ok().body(response.getStream().getDataSource().getInputStream().readAllBytes());
    }


    private byte[] readPdfDocoument() {
        byte[] byteArray = null;
        try {


            InputStream is = getClass().getResourceAsStream("/PDF_PROVA.pdf");
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();

            int nRead;
            byte[] data = new byte[16384];

            while ((nRead = is.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, nRead);
            }

            byteArray = buffer.toByteArray();

        } catch (FileNotFoundException e) {
            log.error("File Not found" + e);
        } catch (IOException e) {
            log.error("IO Ex" + e);
        }
        return byteArray;

    }

}
