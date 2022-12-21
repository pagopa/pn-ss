package it.pagopa.pn.ms.be.service.sign;

import it.pagopa.pn.ms.be.service.sign.dto.GenericFileSignRequestV2;
import it.pagopa.pn.ms.be.service.sign.dto.GenericFileSignReturnV2;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;

@ExtendWith(MockitoExtension.class)
public class GenericFileServiceSignTest {


    @InjectMocks
    ServiceSignImpl service;

    @Mock
    SignServiceSoap signServiceSoap;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testGenericFileSignedAndMarked(){
        GenericFileSignRequestV2 input = new GenericFileSignRequestV2();
        Mockito.when(signServiceSoap.callGenericFile(Mockito.any())).thenReturn( new GenericFileSignReturnV2());
        GenericFileSignReturnV2 reponse = service.pkcs7signV2(input);
        Assertions.assertNotNull(reponse);
    }
    @Test
    public void testGenericFileCorrupted(){
        GenericFileSignRequestV2 input = new GenericFileSignRequestV2();
        GenericFileSignReturnV2  mockResp = new GenericFileSignReturnV2();
        mockResp.setCode("0002");mockResp.setDescription("STREAM EMPTY" );
        Mockito.when(signServiceSoap.callGenericFile(Mockito.any())).thenReturn(mockResp);

        GenericFileSignReturnV2 response = service.pkcs7signV2(input);
        Assertions.assertEquals(response.getCode(),"0002");
        Assertions.assertEquals(response.getDescription(),"STREAM EMPTY" );

    }
    @Test
    public void testGenericFileTranspotType(){
        GenericFileSignRequestV2 input = new GenericFileSignRequestV2();
        GenericFileSignReturnV2  mockResp = new GenericFileSignReturnV2();
        mockResp.setCode("0002");mockResp.setDescription("Incorrect parameters for the type of transport indicated"  );
        Mockito.when(signServiceSoap.callGenericFile(Mockito.any())).thenReturn(mockResp);

        GenericFileSignReturnV2 reponse = service.pkcs7signV2(input);
        Assertions.assertEquals(reponse.getCode(),"0002");
        Assertions.assertEquals(reponse.getDescription(),"Incorrect parameters for the type of transport indicated" );
    }
    @Test
    public void testGenericFileLoginKO(){
        GenericFileSignRequestV2 input = new GenericFileSignRequestV2();
        GenericFileSignReturnV2  mockResp = new GenericFileSignReturnV2();
        mockResp.setCode("0003");mockResp.setDescription("Error during the credential verification phase" );
        Mockito.when(signServiceSoap.callGenericFile(Mockito.any())).thenReturn(mockResp);


        GenericFileSignReturnV2 reponse = service.pkcs7signV2(input);

        Assertions.assertEquals(reponse.getCode(),"0003");
        Assertions.assertEquals(reponse.getDescription(),"Error during the credential verification phase" );
    }
    @Test
    public void testGenericFileTranspotTypeEmpty(){
        GenericFileSignRequestV2 input = new GenericFileSignRequestV2();
        GenericFileSignReturnV2  mockResp = new GenericFileSignReturnV2();
        mockResp.setCode("0005");mockResp.setDescription("Invalid type of transport" );
        Mockito.when(signServiceSoap.callGenericFile(Mockito.any())).thenReturn(mockResp);

        GenericFileSignReturnV2 reponse = service.pkcs7signV2(input);
        Assertions.assertEquals(reponse.getCode(),"0005");
        Assertions.assertEquals(reponse.getDescription(),"Invalid type of transport" );
    }
    @Test
    public void testGenericFileUrlIncorrect(){
        GenericFileSignRequestV2 input = new GenericFileSignRequestV2();
        GenericFileSignReturnV2  mockResp = new GenericFileSignReturnV2();
        mockResp.setCode("500");mockResp.setDescription("Connection refused" );
        Mockito.when(signServiceSoap.callGenericFile(Mockito.any())).thenReturn(mockResp);

        GenericFileSignReturnV2 reponse = service.pkcs7signV2(input);
        Assertions.assertEquals(reponse.getCode(),"500");
        Assertions.assertEquals(reponse.getDescription(),"Connection refused" );
    }
    @Test
    public void testGenericFileCertIdNotcorrect(){
        GenericFileSignRequestV2 input = new GenericFileSignRequestV2();
        GenericFileSignReturnV2  mockResp = new GenericFileSignReturnV2();
        mockResp.setCode("0001");mockResp.setDescription("Generic error in the signature process" );
        Mockito.when(signServiceSoap.callGenericFile(Mockito.any())).thenReturn(mockResp);

        GenericFileSignReturnV2 reponse = service.pkcs7signV2(input);
        Assertions.assertEquals(reponse.getCode(),"0001");
        Assertions.assertEquals(reponse.getDescription(),"Generic error in the signature process" );
    }
    @Test
    public void testGenericFileSignedNotMarked(){
        GenericFileSignRequestV2 input = new GenericFileSignRequestV2();
        Mockito.when(signServiceSoap.callGenericFile(Mockito.any())).thenReturn( new GenericFileSignReturnV2());
        GenericFileSignReturnV2 reponse = service.pkcs7signV2(input);
        Assertions.assertNotNull(reponse);
    }





}
