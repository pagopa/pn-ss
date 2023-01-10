package it.pagopa.pn.ms.be.rest;

import it.pagopa.pn.ms.be.service.sign.SignServiceSoap;
import it.pagopa.pn.ms.be.service.sign.wsdl.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.xml.bind.JAXBException;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;

@RestController
@RequestMapping("/aruba")
public class ArubaController {


    @Autowired
    SignServiceSoap signServiceSoap;






    @GetMapping(path = "/pdfsignatureV2", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity <SignReturnV2> pdfsignatureV2(
            @RequestParam(name ="marcatura") Boolean marcatura
    ) throws TypeOfTransportNotImplemented_Exception, JAXBException {

        byte[] pdfDocument = readPdfDocoument();


        SignReturnV2 response = signServiceSoap.singnPdfDocument(pdfDocument,marcatura);

        return ResponseEntity.ok()
                .body(response);
    }

    @GetMapping(path = "/xmlsignature", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity <byte[]> xmlsignature(
            @RequestParam(name ="marcatura") Boolean marcatura) throws TypeOfTransportNotImplemented_Exception, IOException, JAXBException {
        String filePath = "C:///PROGETTI//DGSPA//materiale start//prova.xml";
        byte[] byteArray = Files.readAllBytes(Paths.get(filePath));

        InputStream targetStream = new ByteArrayInputStream(byteArray);

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
