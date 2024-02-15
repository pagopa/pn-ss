package it.pagopa.pn.library.sign.service;

import it.pagopa.pn.library.sign.configurationproperties.PnSignServiceConfigurationProperties;
import it.pagopa.pn.library.sign.pojo.PnSignDocumentResponse;
import it.pagopa.pn.library.sign.service.impl.AlternativeSignProviderService;
import it.pagopa.pn.library.sign.service.impl.ArubaSignProviderService;
import it.pagopa.pn.library.sign.service.impl.PnSignProviderService;
import it.pagopa.pnss.testutils.annotation.SpringBootTestWebEnv;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.Mockito.*;

@SpringBootTestWebEnv
class PnSignServiceTest {

    @MockBean
    private ArubaSignProviderService arubaSignProviderService;
    @MockBean
    private AlternativeSignProviderService alternativeSignProviderService;
    @Autowired
    private PnSignProviderService pnSignProviderService;
    @Autowired
    private PnSignServiceConfigurationProperties pnSignServiceConfigurationProperties;

    private static final String PROVIDER_SWITCH = "providerSwitch";
    private static final String ARUBA_PROVIDER = "aruba";
    private static final String ALTERNATIVE_PROVIDER = "alternative";
    private static final String CONDITIONAL_DATE_PROVIDER_PAST = "aruba;1999-02-01T10:00:00Z;alternative";
    private static final String CONDITIONAL_DATE_PROVIDER_FUTURE = "aruba;2304-02-15T10:00:00Z;alternative";

    @Test
    void arubaProvider_signPdf_ok() {
        ReflectionTestUtils.setField(pnSignServiceConfigurationProperties, PROVIDER_SWITCH, ARUBA_PROVIDER);
        byte[] fileBytes = "file".getBytes();

        when(arubaSignProviderService.signPdfDocument(any(), any())).thenReturn(Mono.just(new PnSignDocumentResponse()));

        Mono<PnSignDocumentResponse> response = pnSignProviderService.signPdfDocument(fileBytes, true);
        StepVerifier.create(response).expectNextCount(1).verifyComplete();

        verify(arubaSignProviderService, times(1)).signPdfDocument(fileBytes, true);
        verify(alternativeSignProviderService, never()).signPdfDocument(any(), anyBoolean());
    }

    @Test
    void arubaProvider_signXml_ok() {
        ReflectionTestUtils.setField(pnSignServiceConfigurationProperties, PROVIDER_SWITCH, ARUBA_PROVIDER);
        byte[] fileBytes = "file".getBytes();

        when(arubaSignProviderService.signXmlDocument(any(), any())).thenReturn(Mono.just(new PnSignDocumentResponse()));

        Mono<PnSignDocumentResponse> response = pnSignProviderService.signXmlDocument(fileBytes, true);
        StepVerifier.create(response).expectNextCount(1).verifyComplete();

        verify(arubaSignProviderService, times(1)).signXmlDocument(fileBytes, true);
        verify(alternativeSignProviderService, never()).signXmlDocument(any(), anyBoolean());
    }

    @Test
    void arubaProvider_pkcs7Signature_ok() {
        ReflectionTestUtils.setField(pnSignServiceConfigurationProperties, PROVIDER_SWITCH, ARUBA_PROVIDER);
        byte[] fileBytes = "file".getBytes();

        when(arubaSignProviderService.pkcs7Signature(any(), any())).thenReturn(Mono.just(new PnSignDocumentResponse()));

        Mono<PnSignDocumentResponse> response = pnSignProviderService.pkcs7Signature(fileBytes, true);
        StepVerifier.create(response).expectNextCount(1).verifyComplete();

        verify(arubaSignProviderService, times(1)).pkcs7Signature(fileBytes, true);
        verify(alternativeSignProviderService, never()).pkcs7Signature(any(), anyBoolean());
    }

    @Test
    void alternativeProvider_signPdf_ok() {
        ReflectionTestUtils.setField(pnSignServiceConfigurationProperties, PROVIDER_SWITCH, ALTERNATIVE_PROVIDER);
        byte[] fileBytes = "file".getBytes();

        when(alternativeSignProviderService.signPdfDocument(any(), any())).thenReturn(Mono.just(new PnSignDocumentResponse()));

        Mono<PnSignDocumentResponse> response = pnSignProviderService.signPdfDocument(fileBytes, true);
        StepVerifier.create(response).expectNextCount(1).verifyComplete();

        verify(alternativeSignProviderService, times(1)).signPdfDocument(fileBytes, true);
        verify(arubaSignProviderService, never()).signPdfDocument(any(), anyBoolean());
    }

    @Test
    void alternativeProvider_signXml_ok() {
        ReflectionTestUtils.setField(pnSignServiceConfigurationProperties, PROVIDER_SWITCH, ALTERNATIVE_PROVIDER);
        byte[] fileBytes = "file".getBytes();

        when(alternativeSignProviderService.signXmlDocument(any(), any())).thenReturn(Mono.just(new PnSignDocumentResponse()));

        Mono<PnSignDocumentResponse> response = pnSignProviderService.signXmlDocument(fileBytes, true);
        StepVerifier.create(response).expectNextCount(1).verifyComplete();

        verify(alternativeSignProviderService, times(1)).signXmlDocument(fileBytes, true);
        verify(arubaSignProviderService, never()).signXmlDocument(any(), anyBoolean());
    }

    @Test
    void alternativeProvider_pkcs7Signature_ok() {
        ReflectionTestUtils.setField(pnSignServiceConfigurationProperties, PROVIDER_SWITCH, ALTERNATIVE_PROVIDER);
        byte[] fileBytes = "file".getBytes();

        when(alternativeSignProviderService.pkcs7Signature(any(), any())).thenReturn(Mono.just(new PnSignDocumentResponse()));

        Mono<PnSignDocumentResponse> response = pnSignProviderService.pkcs7Signature(fileBytes, true);
        StepVerifier.create(response).expectNextCount(1).verifyComplete();

        verify(alternativeSignProviderService, times(1)).pkcs7Signature(fileBytes, true);
        verify(arubaSignProviderService, never()).pkcs7Signature(any(), anyBoolean());
    }

    @Test
    void conditionalDatePastProvider_signPdf_ok() {
        ReflectionTestUtils.setField(pnSignServiceConfigurationProperties, PROVIDER_SWITCH, CONDITIONAL_DATE_PROVIDER_PAST);
        byte[] fileBytes = "file".getBytes();

        when(alternativeSignProviderService.signPdfDocument(any(), any())).thenReturn(Mono.just(new PnSignDocumentResponse()));

        Mono<PnSignDocumentResponse> response = pnSignProviderService.signPdfDocument(fileBytes, true);
        StepVerifier.create(response).expectNextCount(1).verifyComplete();

        verify(alternativeSignProviderService, times(1)).signPdfDocument(fileBytes, true);
        verify(arubaSignProviderService, never()).signPdfDocument(any(), anyBoolean());
    }

    @Test
    void conditionalDateFutureProvider_signPdf_ok() {
        ReflectionTestUtils.setField(pnSignServiceConfigurationProperties, PROVIDER_SWITCH, CONDITIONAL_DATE_PROVIDER_FUTURE);
        byte[] fileBytes = "file".getBytes();

        when(arubaSignProviderService.signPdfDocument(any(), any())).thenReturn(Mono.just(new PnSignDocumentResponse()));

        Mono<PnSignDocumentResponse> response = pnSignProviderService.signPdfDocument(fileBytes, true);
        StepVerifier.create(response).expectNextCount(1).verifyComplete();

        verify(arubaSignProviderService, times(1)).signPdfDocument(fileBytes, true);
        verify(alternativeSignProviderService, never()).signPdfDocument(any(), anyBoolean());
    }



}
