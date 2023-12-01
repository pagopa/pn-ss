package it.pagopa.pnss.transformation.rest.call.aruba;

import it.pagopa.pn.library.sign.service.impl.ArubaSignServiceImpl;
import it.pagopa.pn.library.sign.exception.aruba.ArubaSignException;
import it.pagopa.pnss.testutils.annotation.SpringBootTestWebEnv;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.test.StepVerifier;

@SpringBootTestWebEnv
public class ArubaSignServiceImplTest {

    @Autowired
    ArubaSignServiceImpl arubaSignServiceCall;

    private static final byte[] byteFile = "Stringa di prova".getBytes();
    private static final boolean marcatura = true;

    //Controllare come mockare ArubaSignService ritornando un Future per scrivere i casi di test Ok
    @Test
    void signPdfDocumentKo(){

        var testMono = arubaSignServiceCall.signPdfDocument(byteFile, marcatura);

        StepVerifier.create(testMono).expectError(ArubaSignException.class).verify();
    }

    @Test
    void pkcs7signV2Ko(){
        byte[] pdfFile = "Stringa di prova".getBytes();
        boolean marcatura = true;

        var testMono = arubaSignServiceCall.pkcs7signV2(byteFile, marcatura);

        StepVerifier.create(testMono).expectError(ArubaSignException.class).verify();
    }

    @Test
    void xmlSignatureKo(){
        byte[] pdfFile = "Stringa di prova".getBytes();
        boolean marcatura = true;

        var testMono = arubaSignServiceCall.xmlSignature(byteFile, marcatura);

        StepVerifier.create(testMono).expectError(ArubaSignException.class).verify();
    }
}
