package it.pagopa.pn.ms.be.service.sign;

import it.pagopa.pn.ms.be.service.sign.dto.InputPdfFileSignRequestV2;
import it.pagopa.pn.ms.be.service.sign.dto.PdfFileSignReturnV2;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;


@ExtendWith(MockitoExtension.class)
public class PdfFileServiceSignTest {


    @InjectMocks
    ServiceSignImpl service;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testPDFFileSignedAndMarked(){
        InputPdfFileSignRequestV2 input = new InputPdfFileSignRequestV2();
        PdfFileSignReturnV2 reponse = service.singnPdfDocument(input);
        Assertions.assertNull(reponse);
    }
    @Test
    public void testPDFFileCorrupted(){
        InputPdfFileSignRequestV2 input = new InputPdfFileSignRequestV2();
        PdfFileSignReturnV2 reponse = service.singnPdfDocument(input);
        Assertions.assertEquals(reponse.getCode(),"0002");
        Assertions.assertEquals(reponse.getDescription(),"STREAM EMPTY" );

    }
    @Test
    public void testPDFFileTranspotType(){
        InputPdfFileSignRequestV2 input = new InputPdfFileSignRequestV2();
        PdfFileSignReturnV2 reponse = service.singnPdfDocument(input);
        Assertions.assertEquals(reponse.getCode(),"0002");
        Assertions.assertEquals(reponse.getDescription(),"Incorrect parameters for the type of transport indicated" );
    }
    @Test
    public void testPDFFileLoginKO(){
        InputPdfFileSignRequestV2 input = new InputPdfFileSignRequestV2();
        PdfFileSignReturnV2 reponse = service.singnPdfDocument(input);

        Assertions.assertEquals(reponse.getCode(),"0003");
        Assertions.assertEquals(reponse.getDescription(),"Error during the credential verification phase" );
    }
    @Test
    public void testPDFFileTranspotTypeEmpty(){
        InputPdfFileSignRequestV2 input = new InputPdfFileSignRequestV2();
        PdfFileSignReturnV2 reponse = service.singnPdfDocument(input);
        Assertions.assertEquals(reponse.getCode(),"0005");
        Assertions.assertEquals(reponse.getDescription(),"Invalid type of transport" );
    }
    @Test
    public void testPDFFileUrlIncorrect(){
        InputPdfFileSignRequestV2 input = new InputPdfFileSignRequestV2();
        PdfFileSignReturnV2 reponse = service.singnPdfDocument(input);
        Assertions.assertEquals(reponse.getCode(),"500");
        Assertions.assertEquals(reponse.getDescription(),"Connection refused" );
    }
    @Test
    public void testPDFFileCertIdNotcorrect(){
        InputPdfFileSignRequestV2 input = new InputPdfFileSignRequestV2();
        PdfFileSignReturnV2 reponse = service.singnPdfDocument(input);
        Assertions.assertEquals(reponse.getCode(),"0001");
        Assertions.assertEquals(reponse.getDescription(),"PDF error in the signature process" );
    }
    @Test
    public void testPDFFileSignedNotMarked(){
        InputPdfFileSignRequestV2 input = new InputPdfFileSignRequestV2();
        PdfFileSignReturnV2 reponse = service.singnPdfDocument(input);
        Assertions.assertNull(reponse);
    }





}
