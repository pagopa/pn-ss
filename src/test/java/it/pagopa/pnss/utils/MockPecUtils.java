package it.pagopa.pnss.utils;

import com.namirial.sign.library.service.PnSignServiceImpl;
import it.pagopa.pn.library.exceptions.PnSpapiPermanentErrorException;
import it.pagopa.pn.library.exceptions.PnSpapiTemporaryErrorException;
import it.pagopa.pn.library.sign.pojo.PnSignDocumentResponse;
import it.pagopa.pnss.transformation.wsdl.*;
import reactor.core.publisher.Mono;

import javax.xml.ws.AsyncHandler;
import javax.xml.ws.Response;

import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MockPecUtils {

    private MockPecUtils() {
        throw new IllegalStateException("MockArubaUtils is a utility class");
    }

    public static final String RESP_OK = "Ok";
    public static final String RESP_TEMP = "Temporary";
    public static final String RESP_PERM = "Permanent";

    private static final String TEMP_ERROR = "test temporary exception";
    private static final String PERM_ERROR = "test permanent exception";

    public static void mockArubaPdfSignatureV2Async(ArubaSignService arubaSignServiceClient, String returnStatus, byte[] returnFileBytes, String resultType) {
        when(arubaSignServiceClient.pdfsignatureV2Async(any(), any())).thenAnswer(invocation -> {
                    switch (resultType) {
                        case RESP_OK:
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
                        case RESP_TEMP:
                            return new PnSpapiTemporaryErrorException(TEMP_ERROR);
                        case RESP_PERM:
                            return new PnSpapiPermanentErrorException(PERM_ERROR);
                        default:
                            return new Exception();
                    }
                });
    }

    public static void mockAltProvPdfSignatureV2Async(PnSignServiceImpl namirialSignProviderService,String resultType) {
        when(namirialSignProviderService.signPdfDocument(any(), any())).thenAnswer(invocation -> {
            switch (resultType) {
                case RESP_OK:
                    return Mono.just(new PnSignDocumentResponse());
                case RESP_TEMP:
                    return Mono.error(new PnSpapiTemporaryErrorException(TEMP_ERROR));
                case RESP_PERM:
                    return Mono.error(new PnSpapiPermanentErrorException(PERM_ERROR));
                default:
                    return Mono.error(new Exception());
            }
        });
    }

    public static void mockArubaXmlSignatureAsync(ArubaSignService arubaSignServiceClient, String returnStatus, byte[] returnFileBytes, String resultType) {
        when(arubaSignServiceClient.xmlsignatureAsync(any(), any())).thenAnswer(invocation -> {
            switch (resultType) {
                case RESP_OK:
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
                case RESP_TEMP:
                    return new PnSpapiTemporaryErrorException(TEMP_ERROR);
                case RESP_PERM:
                    return new PnSpapiPermanentErrorException(PERM_ERROR);
                default:
                    return new Exception();
            }
        });
    }

    public static void mockAltProvXmlSignatureAsync(PnSignServiceImpl namirialSignProviderService, String resultType) {
        when(namirialSignProviderService.signXmlDocument(any(), any())).thenAnswer(invocation -> {
            switch (resultType) {
                case RESP_OK:
                    return Mono.just(new PnSignDocumentResponse());
                case RESP_TEMP:
                    return Mono.error(new PnSpapiTemporaryErrorException(TEMP_ERROR));
                case RESP_PERM:
                    return Mono.error(new PnSpapiPermanentErrorException(PERM_ERROR));
                default:
                    return Mono.error(new Exception());
            }
        });
    }

    public static void mockArubaPkcs7SignV2Async(ArubaSignService arubaSignServiceClient, String returnStatus, byte[] returnFileBytes, String resultType) {
        when(arubaSignServiceClient.pkcs7SignV2Async(any(), any())).thenAnswer(invocation -> {
            switch (resultType) {
                case RESP_OK:
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
                case RESP_TEMP:
                    return new PnSpapiTemporaryErrorException(TEMP_ERROR);
                case RESP_PERM:
                    return new PnSpapiPermanentErrorException(PERM_ERROR);
                default:
                    return new Exception();
            }
        });
    }

    public static void mockAltProvPkcs7SignV2Async(PnSignServiceImpl namirialSignProviderService,String resultType) {
        when(namirialSignProviderService.pkcs7Signature(any(), any())).thenAnswer(invocation -> {
            switch (resultType) {
                case RESP_OK:
                    return Mono.just(new PnSignDocumentResponse());
                case RESP_TEMP:
                    return Mono.error(new PnSpapiTemporaryErrorException(RESP_TEMP));
                case RESP_PERM:
                    return Mono.error(new PnSpapiPermanentErrorException(RESP_PERM));
                default:
                    return Mono.error(new Exception());
            }
        });
    }

}
