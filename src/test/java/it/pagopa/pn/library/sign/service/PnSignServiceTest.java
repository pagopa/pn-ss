package it.pagopa.pn.library.sign.service;

import com.namirial.sign.library.service.PnSignServiceImpl;
import it.pagopa.pn.library.exceptions.PnSpapiPermanentErrorException;
import it.pagopa.pn.library.sign.configurationproperties.PnSignServiceConfigurationProperties;
import it.pagopa.pn.library.sign.exception.MaxRetryExceededException;
import it.pagopa.pn.library.sign.pojo.PnSignDocumentResponse;
import it.pagopa.pn.library.sign.service.impl.ArubaSignProviderService;
import it.pagopa.pn.library.sign.service.impl.PnSignProviderService;
import it.pagopa.pnss.testutils.annotation.SpringBootTestWebEnv;
import it.pagopa.pnss.transformation.wsdl.ArubaSignService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import static it.pagopa.pnss.utils.MockPecUtils.*;
import static org.mockito.Mockito.*;

@SpringBootTestWebEnv
class PnSignServiceTest {

    @SpyBean
    private ArubaSignProviderService arubaSignProviderService;
    @SpyBean
    private PnSignServiceImpl namirialSignProviderService;
    @SpyBean
    private PnSignProviderService pnSignProviderService;
    @Autowired
    private PnSignServiceConfigurationProperties pnSignServiceConfigurationProperties;
    @MockBean
    private ArubaSignService arubaSignServiceClient;

    private static final String PROVIDER_SWITCH = "providerSwitch";
    private static final String CONDITIONAL_DATE_PROVIDER_PAST = "1999-02-01T10:00:00Z;aruba";
    private static final String CONDITIONAL_DATE_PROVIDER_FUTURE = "1999-02-01T10:00:00Z;aruba,2004-02-15T10:00:00Z;namirial";

    @Test
    void arubaProvider_signPdf_ok() {
        ReflectionTestUtils.setField(pnSignServiceConfigurationProperties, PROVIDER_SWITCH, CONDITIONAL_DATE_PROVIDER_PAST);
        byte[] fileBytes = "file".getBytes();

        mockArubaPdfSignatureV2Async(arubaSignServiceClient, "ok", fileBytes, RESP_OK);

        Mono<PnSignDocumentResponse> response = pnSignProviderService.signPdfDocument(fileBytes, true);
        StepVerifier.create(response).expectNextCount(1).verifyComplete();

        verify(arubaSignProviderService, times(1)).signPdfDocument(fileBytes, true);
        verify(namirialSignProviderService, never()).signPdfDocument(any(), anyBoolean());
    }

    @Test
    void arubaProvider_signPdf_Temporary() {
        ReflectionTestUtils.setField(pnSignServiceConfigurationProperties, PROVIDER_SWITCH, CONDITIONAL_DATE_PROVIDER_PAST);
        byte[] fileBytes = "file".getBytes();

        mockArubaPdfSignatureV2Async(arubaSignServiceClient, "ko", fileBytes, RESP_TEMP);

        Mono<PnSignDocumentResponse> response = pnSignProviderService.signPdfDocument(fileBytes, true);
        StepVerifier.create(response).expectError(MaxRetryExceededException.class).verify();

        verify(arubaSignProviderService, times(1)).signPdfDocument(fileBytes, true);
        verify(namirialSignProviderService, never()).signPdfDocument(any(), anyBoolean());
    }

    @Test
    void arubaProvider_signXml_ok() {
        ReflectionTestUtils.setField(pnSignServiceConfigurationProperties, PROVIDER_SWITCH, CONDITIONAL_DATE_PROVIDER_PAST);
        byte[] fileBytes = "file".getBytes();

        mockArubaXmlSignatureAsync(arubaSignServiceClient, "ok", fileBytes, RESP_OK);

        Mono<PnSignDocumentResponse> response = pnSignProviderService.signXmlDocument(fileBytes, true);
        StepVerifier.create(response).expectNextCount(1).verifyComplete();

        verify(arubaSignProviderService, times(1)).signXmlDocument(fileBytes, true);
        verify(namirialSignProviderService, never()).signXmlDocument(any(), anyBoolean());
    }

    @Test
    void arubaProvider_signXml_Temporary() {
        ReflectionTestUtils.setField(pnSignServiceConfigurationProperties, PROVIDER_SWITCH, CONDITIONAL_DATE_PROVIDER_PAST);
        byte[] fileBytes = "file".getBytes();

        mockArubaXmlSignatureAsync(arubaSignServiceClient, "ko", fileBytes, RESP_TEMP);

        Mono<PnSignDocumentResponse> response = pnSignProviderService.signXmlDocument(fileBytes, true);
        StepVerifier.create(response).expectError(MaxRetryExceededException.class).verify();

        verify(arubaSignProviderService, times(1)).signXmlDocument(fileBytes, true);
        verify(namirialSignProviderService, never()).signXmlDocument(any(), anyBoolean());
    }

    @Test
    void arubaProvider_pkcs7Signature_ok() {
        ReflectionTestUtils.setField(pnSignServiceConfigurationProperties, PROVIDER_SWITCH, CONDITIONAL_DATE_PROVIDER_PAST);
        byte[] fileBytes = "file".getBytes();

        mockArubaPkcs7SignV2Async(arubaSignServiceClient, "ok", fileBytes, RESP_OK);

        Mono<PnSignDocumentResponse> response = pnSignProviderService.pkcs7Signature(fileBytes, true);
        StepVerifier.create(response).expectNextCount(1).verifyComplete();

        verify(arubaSignProviderService, times(1)).pkcs7Signature(fileBytes, true);
        verify(namirialSignProviderService, never()).pkcs7Signature(any(), anyBoolean());
    }

    @Test
    void arubaProvider_pkcs7Signature_Temporary() {
        ReflectionTestUtils.setField(pnSignServiceConfigurationProperties, PROVIDER_SWITCH, CONDITIONAL_DATE_PROVIDER_PAST);
        byte[] fileBytes = "file".getBytes();

        mockArubaPkcs7SignV2Async(arubaSignServiceClient, "ko", fileBytes, RESP_TEMP);

        Mono<PnSignDocumentResponse> response = pnSignProviderService.pkcs7Signature(fileBytes, true);
        StepVerifier.create(response).expectError(MaxRetryExceededException.class).verify();

        verify(arubaSignProviderService, times(1)).pkcs7Signature(fileBytes, true);
        verify(namirialSignProviderService, never()).pkcs7Signature(any(), anyBoolean());
    }

    @Test
    void namirialProvider_signPdf_ok() {
        ReflectionTestUtils.setField(pnSignServiceConfigurationProperties, PROVIDER_SWITCH, CONDITIONAL_DATE_PROVIDER_FUTURE);
        byte[] fileBytes = "file".getBytes();

        mockAltProvPdfSignatureV2Async(namirialSignProviderService, RESP_OK);

        Mono<PnSignDocumentResponse> response = pnSignProviderService.signPdfDocument(fileBytes, true);
        StepVerifier.create(response).expectNextCount(1).verifyComplete();

        verify(namirialSignProviderService, times(1)).signPdfDocument(fileBytes, true);
        verify(arubaSignProviderService, never()).signPdfDocument(any(), anyBoolean());
    }

    @Test
    void namirialProvider_signPdf_Temp() {
        ReflectionTestUtils.setField(pnSignServiceConfigurationProperties, PROVIDER_SWITCH, CONDITIONAL_DATE_PROVIDER_FUTURE);
        byte[] fileBytes = "file".getBytes();

        mockAltProvPdfSignatureV2Async(namirialSignProviderService,  RESP_TEMP);

        Mono<PnSignDocumentResponse> response = pnSignProviderService.signPdfDocument(fileBytes, true);
        StepVerifier.create(response).expectError(MaxRetryExceededException.class).verify();

        verify(namirialSignProviderService, times(1)).signPdfDocument(fileBytes, true);
        verify(arubaSignProviderService, never()).signPdfDocument(any(), anyBoolean());
    }

    @Test
    void namirialProvider_signPdf_Perm() {
        ReflectionTestUtils.setField(pnSignServiceConfigurationProperties, PROVIDER_SWITCH, CONDITIONAL_DATE_PROVIDER_FUTURE);
        byte[] fileBytes = "file".getBytes();

        mockAltProvPdfSignatureV2Async(namirialSignProviderService,  RESP_PERM);

        Mono<PnSignDocumentResponse> response = pnSignProviderService.signPdfDocument(fileBytes, true);
        StepVerifier.create(response).expectError(PnSpapiPermanentErrorException.class).verify();

        verify(namirialSignProviderService, times(1)).signPdfDocument(fileBytes, true);
        verify(arubaSignProviderService, never()).signPdfDocument(any(), anyBoolean());
    }

    @Test
    void namirialProvider_signXml_ok() {
        ReflectionTestUtils.setField(pnSignServiceConfigurationProperties, PROVIDER_SWITCH, CONDITIONAL_DATE_PROVIDER_FUTURE);
        byte[] fileBytes = "file".getBytes();

        mockAltProvXmlSignatureAsync(namirialSignProviderService,  RESP_OK);

        Mono<PnSignDocumentResponse> response = pnSignProviderService.signXmlDocument(fileBytes, true);
        StepVerifier.create(response).expectNextCount(1).verifyComplete();

        verify(namirialSignProviderService, times(1)).signXmlDocument(fileBytes, true);
        verify(arubaSignProviderService, never()).signXmlDocument(any(), anyBoolean());
    }

    @Test
    void namirialProvider_signXml_Temp() {
        ReflectionTestUtils.setField(pnSignServiceConfigurationProperties, PROVIDER_SWITCH, CONDITIONAL_DATE_PROVIDER_FUTURE);
        byte[] fileBytes = "file".getBytes();

        mockAltProvXmlSignatureAsync(namirialSignProviderService, RESP_TEMP);

        Mono<PnSignDocumentResponse> response = pnSignProviderService.signXmlDocument(fileBytes, true);
        StepVerifier.create(response).expectError(MaxRetryExceededException.class).verify();

        verify(namirialSignProviderService, times(1)).signXmlDocument(fileBytes, true);
        verify(arubaSignProviderService, never()).signXmlDocument(any(), anyBoolean());
    }

    @Test
    void namirialProvider_signXml_Perm() {
        ReflectionTestUtils.setField(pnSignServiceConfigurationProperties, PROVIDER_SWITCH, CONDITIONAL_DATE_PROVIDER_FUTURE);
        byte[] fileBytes = "file".getBytes();

        mockAltProvXmlSignatureAsync(namirialSignProviderService, RESP_PERM);

        Mono<PnSignDocumentResponse> response = pnSignProviderService.signXmlDocument(fileBytes, true);
        StepVerifier.create(response).expectError(PnSpapiPermanentErrorException.class).verify();

        verify(namirialSignProviderService, times(1)).signXmlDocument(fileBytes, true);
        verify(arubaSignProviderService, never()).signXmlDocument(any(), anyBoolean());
    }

    @Test
    void namirialProvider_pkcs7Signature_ok() {
        ReflectionTestUtils.setField(pnSignServiceConfigurationProperties, PROVIDER_SWITCH, CONDITIONAL_DATE_PROVIDER_FUTURE);
        byte[] fileBytes = "file".getBytes();

        mockAltProvPkcs7SignV2Async(namirialSignProviderService, RESP_OK);

        Mono<PnSignDocumentResponse> response = pnSignProviderService.pkcs7Signature(fileBytes, true);
        StepVerifier.create(response).expectNextCount(1).verifyComplete();

        verify(namirialSignProviderService, times(1)).pkcs7Signature(fileBytes, true);
        verify(arubaSignProviderService, never()).pkcs7Signature(any(), anyBoolean());
    }

    @Test
    void namirialProvider_pkcs7Signature_Temp() {
        ReflectionTestUtils.setField(pnSignServiceConfigurationProperties, PROVIDER_SWITCH, CONDITIONAL_DATE_PROVIDER_FUTURE);
        byte[] fileBytes = "file".getBytes();

        mockAltProvPkcs7SignV2Async(namirialSignProviderService, RESP_TEMP);

        Mono<PnSignDocumentResponse> response = pnSignProviderService.pkcs7Signature(fileBytes, true);
        StepVerifier.create(response).expectError(MaxRetryExceededException.class).verify();

        verify(namirialSignProviderService, times(1)).pkcs7Signature(fileBytes, true);
        verify(arubaSignProviderService, never()).pkcs7Signature(any(), anyBoolean());
    }

    @Test
    void namirialProvider_pkcs7Signature_Perm() {
        ReflectionTestUtils.setField(pnSignServiceConfigurationProperties, PROVIDER_SWITCH, CONDITIONAL_DATE_PROVIDER_FUTURE);
        byte[] fileBytes = "file".getBytes();

        mockAltProvPkcs7SignV2Async(namirialSignProviderService, RESP_PERM);

        Mono<PnSignDocumentResponse> response = pnSignProviderService.pkcs7Signature(fileBytes, true);
        StepVerifier.create(response).expectError(PnSpapiPermanentErrorException.class).verify();

        verify(namirialSignProviderService, times(1)).pkcs7Signature(fileBytes, true);
        verify(arubaSignProviderService, never()).pkcs7Signature(any(), anyBoolean());
    }

    @Test
    void conditionalDateFutureProvider_signPdf_ok() {
        ReflectionTestUtils.setField(pnSignServiceConfigurationProperties, PROVIDER_SWITCH, CONDITIONAL_DATE_PROVIDER_FUTURE);
        byte[] fileBytes = "file".getBytes();

        mockAltProvPdfSignatureV2Async(namirialSignProviderService,RESP_OK);

        Mono<PnSignDocumentResponse> response = pnSignProviderService.signPdfDocument(fileBytes, true);
        StepVerifier.create(response).expectNextCount(1).verifyComplete();

        verify(namirialSignProviderService, times(1)).signPdfDocument(fileBytes, true);
        verify(arubaSignProviderService, never()).signPdfDocument(any(), anyBoolean());
    }

    @Test
    void conditionalDatePastProvider_signPdf_ok() {
        ReflectionTestUtils.setField(pnSignServiceConfigurationProperties, PROVIDER_SWITCH, CONDITIONAL_DATE_PROVIDER_PAST);
        byte[] fileBytes = "file".getBytes();

        mockArubaPdfSignatureV2Async(arubaSignServiceClient, "ok", fileBytes, RESP_OK);

        Mono<PnSignDocumentResponse> response = pnSignProviderService.signPdfDocument(fileBytes, true);
        StepVerifier.create(response).expectNextCount(1).verifyComplete();

        verify(arubaSignProviderService, times(1)).signPdfDocument(fileBytes, true);
        verify(namirialSignProviderService, never()).signPdfDocument(any(), anyBoolean());
    }

}
