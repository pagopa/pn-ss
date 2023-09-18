package it.pagopa.pnss.transformation.rest.call.aruba;

import it.pagopa.pnss.testutils.annotation.SpringBootTestWebEnv;
import it.pagopa.pnss.transformation.wsdl.ArubaSignService;
import it.pagopa.pnss.transformation.wsdl.PdfsignatureV2;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.testcontainers.shaded.org.apache.commons.lang3.concurrent.ConcurrentUtils;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTestWebEnv
public class ArubaSignServiceCallImplTest {

    @Autowired
    ArubaSignServiceCallImpl arubaSignServiceCall;

    @Autowired
    ArubaSignService arubaSignService;

    @Test
    void signPdfDocumentOk(){
        byte[] pdfFile = "Stringa di prova".getBytes();
        boolean marcatura = true;

        Future<?> future = ConcurrentUtils.constantFuture(T myValue);
        Future<?> future = future.get(1, TimeUnit.);

        when(arubaSignService.pdfsignatureV2Async(any(PdfsignatureV2.class), any())).thenReturn(Mono.just(future));

        var testMono = arubaSignServiceCall.signPdfDocument(pdfFile, marcatura);

        StepVerifier.create(testMono).expectNextCount(1).verifyComplete();
    }
}
