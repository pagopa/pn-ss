package it.pagopa.pn.ms.be.service.sign;

import it.pagopa.pn.ms.be.service.sign.dto.GenericFileSignRequestV2;
import it.pagopa.pn.ms.be.service.sign.dto.GenericFileSignReturnV2;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class GenericFileServiceSignTest {


    @InjectMocks
    ServiceSignImpl service;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testGenericFileSignedAndMarked(){
        GenericFileSignRequestV2 input = new GenericFileSignRequestV2();
        GenericFileSignReturnV2 reponse = service.pkcs7signV2(input);
        Assertions.assertNull(reponse);
    }
    @Test
    public void testGenericFileCorrupted(){
        GenericFileSignRequestV2 input = new GenericFileSignRequestV2();
        GenericFileSignReturnV2 reponse = service.pkcs7signV2(input);
        Assertions.assertEquals(reponse.getCode(),"0002");
        Assertions.assertEquals(reponse.getDescription(),"STREAM EMPTY" );

    }
    @Test
    public void testGenericFileTranspotType(){
        GenericFileSignRequestV2 input = new GenericFileSignRequestV2();
        GenericFileSignReturnV2 reponse = service.pkcs7signV2(input);
        Assertions.assertEquals(reponse.getCode(),"0002");
        Assertions.assertEquals(reponse.getDescription(),"Incorrect parameters for the type of transport indicated" );
    }
    @Test
    public void testGenericFileLoginKO(){
        GenericFileSignRequestV2 input = new GenericFileSignRequestV2();
        GenericFileSignReturnV2 reponse = service.pkcs7signV2(input);

        Assertions.assertEquals(reponse.getCode(),"0003");
        Assertions.assertEquals(reponse.getDescription(),"Error during the credential verification phase" );
    }
    @Test
    public void testGenericFileTranspotTypeEmpty(){
        GenericFileSignRequestV2 input = new GenericFileSignRequestV2();
        GenericFileSignReturnV2 reponse = service.pkcs7signV2(input);
        Assertions.assertEquals(reponse.getCode(),"0005");
        Assertions.assertEquals(reponse.getDescription(),"Invalid type of transport" );
    }
    @Test
    public void testGenericFileUrlIncorrect(){
        GenericFileSignRequestV2 input = new GenericFileSignRequestV2();
        GenericFileSignReturnV2 reponse = service.pkcs7signV2(input);
        Assertions.assertEquals(reponse.getCode(),"500");
        Assertions.assertEquals(reponse.getDescription(),"Connection refused" );
    }
    @Test
    public void testGenericFileCertIdNotcorrect(){
        GenericFileSignRequestV2 input = new GenericFileSignRequestV2();
        GenericFileSignReturnV2 reponse = service.pkcs7signV2(input);
        Assertions.assertEquals(reponse.getCode(),"0001");
        Assertions.assertEquals(reponse.getDescription(),"Generic error in the signature process" );
    }
    @Test
    public void testGenericFileSignedNotMarked(){
        GenericFileSignRequestV2 input = new GenericFileSignRequestV2();
        GenericFileSignReturnV2 reponse = service.pkcs7signV2(input);
        Assertions.assertNull(reponse);
    }





}
