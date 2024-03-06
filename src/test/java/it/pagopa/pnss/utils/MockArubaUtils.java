package it.pagopa.pnss.utils;

import it.pagopa.pnss.transformation.wsdl.*;
import javax.xml.ws.AsyncHandler;
import javax.xml.ws.Response;

import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MockArubaUtils {

    private MockArubaUtils() {
        throw new IllegalStateException("MockArubaUtils is a utility class");
    }

    public static void mockArubaPdfSignatureV2Async(ArubaSignService arubaSignServiceClient, String returnStatus, byte[] returnFileBytes) {
        when(arubaSignServiceClient.pdfsignatureV2Async(any(), any())).thenAnswer(invocation -> {
            AsyncHandler handler = invocation.getArgument(1, AsyncHandler.class);
            Response<PdfsignatureV2Response> response = mock(Response.class);

            SignReturnV2 signReturnV2 = new SignReturnV2();
            signReturnV2.setStatus(returnStatus);
            signReturnV2.setBinaryoutput(returnFileBytes);

            PdfsignatureV2Response pdfsignatureV2Response = new PdfsignatureV2Response();
            pdfsignatureV2Response.setReturn(signReturnV2);

            when(response.get()).thenReturn(pdfsignatureV2Response);
            handler.handleResponse(response);
            return CompletableFuture.completedFuture(null);
        });
    }

    public static void mockArubaXmlSignatureAsync(ArubaSignService arubaSignServiceClient, String returnStatus, byte[] returnFileBytes) {
        when(arubaSignServiceClient.xmlsignatureAsync(any(), any())).thenAnswer(invocation -> {
            AsyncHandler<XmlsignatureResponse> handler = invocation.getArgument(1, AsyncHandler.class);
            Response<XmlsignatureResponse> response = mock(Response.class);

            SignReturnV2 signReturnV2 = new SignReturnV2();
            signReturnV2.setStatus(returnStatus);
            signReturnV2.setBinaryoutput(returnFileBytes);

            XmlsignatureResponse xmlsignatureResponse = new XmlsignatureResponse();
            xmlsignatureResponse.setReturn(signReturnV2);

            when(response.get()).thenReturn(xmlsignatureResponse);
            handler.handleResponse(response);
            return CompletableFuture.completedFuture(null);
        });
    }

    public static void mockArubaPkcs7SignV2Async(ArubaSignService arubaSignServiceClient, String returnStatus, byte[] returnFileBytes) {
        when(arubaSignServiceClient.pkcs7SignV2Async(any(), any())).thenAnswer(invocation -> {
            AsyncHandler<Pkcs7SignV2Response> handler = invocation.getArgument(1, AsyncHandler.class);
            Response<Pkcs7SignV2Response> response = mock(Response.class);

            SignReturnV2 signReturnV2 = new SignReturnV2();
            signReturnV2.setStatus(returnStatus);
            signReturnV2.setBinaryoutput(returnFileBytes);

            Pkcs7SignV2Response pkcs7SignV2Response = new Pkcs7SignV2Response();
            pkcs7SignV2Response.setReturn(signReturnV2);

            when(response.get()).thenReturn(pkcs7SignV2Response);
            handler.handleResponse(response);
            return CompletableFuture.completedFuture(null);
        });
    }

}
