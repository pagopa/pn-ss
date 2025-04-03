package it.pagopa.pn.library.sign.service;

import com.namirial.sign.library.service.PnSignServiceImpl;
import it.pagopa.pn.library.exceptions.PnSpapiPermanentErrorException;
import it.pagopa.pn.ss.dummy.sign.service.PnDummySignServiceImpl;
import it.pagopa.pnss.common.service.impl.CloudWatchMetricsService;
import it.pagopa.pnss.configuration.cloudwatch.CloudWatchMetricPublisherConfiguration;
import it.pagopa.pn.library.sign.configurationproperties.PnSignServiceConfigurationProperties;
import it.pagopa.pn.library.sign.exception.MaxRetryExceededException;
import it.pagopa.pn.library.sign.pojo.PnSignDocumentResponse;
import it.pagopa.pn.library.sign.service.impl.ArubaSignProviderService;
import it.pagopa.pn.library.sign.service.impl.PnSignProviderService;
import it.pagopa.pnss.testutils.annotation.SpringBootTestWebEnv;
import it.pagopa.pnss.transformation.wsdl.ArubaSignService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static it.pagopa.pn.library.sign.pojo.SignatureType.*;
import static it.pagopa.pnss.utils.MockPecUtils.*;
import static org.mockito.Mockito.*;

@SpringBootTestWebEnv
class PnSignServiceTest {

    @SpyBean
    private ArubaSignProviderService arubaSignProviderService;
    @SpyBean
    private PnSignServiceImpl namirialSignProviderService;
    @SpyBean
    private PnDummySignServiceImpl dummySignProviderService;
    @SpyBean
    private PnSignProviderService pnSignProviderService;
    @Autowired
    private PnSignServiceConfigurationProperties pnSignServiceConfigurationProperties;
    @SpyBean
    private CloudWatchMetricPublisherConfiguration cloudWatchMetricPublisherConfiguration;
    @SpyBean
    private CloudWatchMetricsService cloudWatchMetricsService;
    @MockBean
    private ArubaSignService arubaSignServiceClient;
    @Value("${pn.sign.cloudwatch.namespace.aruba}")
    private String arubaNamespace;
    @Value("${pn.sign.cloudwatch.namespace.namirial}")
    private String namirialNamespace;

    private static final String PROVIDER_SWITCH = "providerSwitch";
    private static final String CONDITIONAL_DATE_PROVIDER_PAST = "1999-02-01T10:00:00Z;aruba";
    private static final String CONDITIONAL_DATE_PROVIDER_DUMMY = "1999-02-01T10:00:00Z;dummy";
    private static final String CONDITIONAL_DATE_PROVIDER_FUTURE = "1999-02-01T10:00:00Z;aruba,2004-02-15T10:00:00Z;namirial";
    private static final byte[] fileBytes = "file".getBytes();

    @Test
    void arubaProvider_signPdf_ok() {
        ReflectionTestUtils.setField(pnSignServiceConfigurationProperties, PROVIDER_SWITCH, CONDITIONAL_DATE_PROVIDER_PAST);

        mockArubaPdfSignatureV2Async(arubaSignServiceClient, "ok", fileBytes, RESP_OK);

        Mono<PnSignDocumentResponse> response = pnSignProviderService.signPdfDocument(fileBytes, true);
        StepVerifier.create(response).expectNextCount(1).verifyComplete();

        verify(arubaSignProviderService, times(1)).signPdfDocument(fileBytes, true);
        verify(namirialSignProviderService, never()).signPdfDocument(any(), anyBoolean());
        verify(dummySignProviderService, never()).signPdfDocument(any(), anyBoolean());
        verify(cloudWatchMetricsService, times(1)).publishResponseTime(eq(arubaNamespace), eq(PADES.getValue()), anyLong(), anyLong());
    }

    @Test
    void arubaProvider_signPdf_Temporary() {
        ReflectionTestUtils.setField(pnSignServiceConfigurationProperties, PROVIDER_SWITCH, CONDITIONAL_DATE_PROVIDER_PAST);

        mockArubaPdfSignatureV2Async(arubaSignServiceClient, "ko", fileBytes, RESP_TEMP);

        Mono<PnSignDocumentResponse> response = pnSignProviderService.signPdfDocument(fileBytes, true);
        StepVerifier.create(response).expectError(MaxRetryExceededException.class).verify();

        verify(arubaSignProviderService, times(1)).signPdfDocument(fileBytes, true);
        verify(namirialSignProviderService, never()).signPdfDocument(any(), anyBoolean());
        verify(dummySignProviderService, never()).signPdfDocument(any(), anyBoolean());
    }

    @Test
    void arubaProvider_signXml_ok() {
        ReflectionTestUtils.setField(pnSignServiceConfigurationProperties, PROVIDER_SWITCH, CONDITIONAL_DATE_PROVIDER_PAST);

        mockArubaXmlSignatureAsync(arubaSignServiceClient, "ok", fileBytes, RESP_OK);

        Mono<PnSignDocumentResponse> response = pnSignProviderService.signXmlDocument(fileBytes, true);
        StepVerifier.create(response).expectNextCount(1).verifyComplete();

        verify(arubaSignProviderService, times(1)).signXmlDocument(fileBytes, true);
        verify(namirialSignProviderService, never()).signXmlDocument(any(), anyBoolean());
        verify(dummySignProviderService, never()).signXmlDocument(any(), anyBoolean());
        verify(cloudWatchMetricsService, times(1)).publishResponseTime(eq(arubaNamespace), eq(XADES.getValue()), anyLong(), anyLong());
    }

    @Test
    void arubaProvider_signXml_Temporary() {
        ReflectionTestUtils.setField(pnSignServiceConfigurationProperties, PROVIDER_SWITCH, CONDITIONAL_DATE_PROVIDER_PAST);

        mockArubaXmlSignatureAsync(arubaSignServiceClient, "ko", fileBytes, RESP_TEMP);

        Mono<PnSignDocumentResponse> response = pnSignProviderService.signXmlDocument(fileBytes, true);
        StepVerifier.create(response).expectError(MaxRetryExceededException.class).verify();

        verify(arubaSignProviderService, times(1)).signXmlDocument(fileBytes, true);
        verify(namirialSignProviderService, never()).signXmlDocument(any(), anyBoolean());
        verify(dummySignProviderService, never()).signXmlDocument(any(), anyBoolean());
    }

    @Test
    void arubaProvider_pkcs7Signature_ok() {
        ReflectionTestUtils.setField(pnSignServiceConfigurationProperties, PROVIDER_SWITCH, CONDITIONAL_DATE_PROVIDER_PAST);

        mockArubaPkcs7SignV2Async(arubaSignServiceClient, "ok", fileBytes, RESP_OK);
        Mono<PnSignDocumentResponse> response = pnSignProviderService.pkcs7Signature(fileBytes, true);
        StepVerifier.create(response).expectNextCount(1).verifyComplete();

        verify(arubaSignProviderService, times(1)).pkcs7Signature(fileBytes, true);
        verify(namirialSignProviderService, never()).pkcs7Signature(any(), anyBoolean());
        verify(dummySignProviderService, never()).pkcs7Signature(any(), anyBoolean());
        verify(cloudWatchMetricsService, times(1)).publishResponseTime(eq(arubaNamespace), eq(CADES.getValue()), anyLong(), anyLong());
    }

    @Test
    void arubaProvider_pkcs7Signature_Temporary() {
        ReflectionTestUtils.setField(pnSignServiceConfigurationProperties, PROVIDER_SWITCH, CONDITIONAL_DATE_PROVIDER_PAST);

        mockArubaPkcs7SignV2Async(arubaSignServiceClient, "ko", fileBytes, RESP_TEMP);

        Mono<PnSignDocumentResponse> response = pnSignProviderService.pkcs7Signature(fileBytes, true);
        StepVerifier.create(response).expectError(MaxRetryExceededException.class).verify();

        verify(arubaSignProviderService, times(1)).pkcs7Signature(fileBytes, true);
        verify(namirialSignProviderService, never()).pkcs7Signature(any(), anyBoolean());
        verify(dummySignProviderService, never()).pkcs7Signature(any(), anyBoolean());
    }

    @Test
    void namirialProvider_signPdf_ok() {
        ReflectionTestUtils.setField(pnSignServiceConfigurationProperties, PROVIDER_SWITCH, CONDITIONAL_DATE_PROVIDER_FUTURE);

        mockAltProvPdfSignatureV2Async(namirialSignProviderService, RESP_OK);

        Mono<PnSignDocumentResponse> response = pnSignProviderService.signPdfDocument(fileBytes, true);
        StepVerifier.create(response).expectNextCount(1).verifyComplete();

        verify(namirialSignProviderService, times(1)).signPdfDocument(fileBytes, true);
        verify(arubaSignProviderService, never()).signPdfDocument(any(), anyBoolean());
        verify(dummySignProviderService, never()).signPdfDocument(any(), anyBoolean());
        verify(cloudWatchMetricsService, times(1)).publishResponseTime(eq(namirialNamespace), eq(PADES.getValue()), anyLong(), anyLong());
    }

    @Test
    void namirialProvider_signPdf_Temp() {
        ReflectionTestUtils.setField(pnSignServiceConfigurationProperties, PROVIDER_SWITCH, CONDITIONAL_DATE_PROVIDER_FUTURE);

        mockAltProvPdfSignatureV2Async(namirialSignProviderService,  RESP_TEMP);

        Mono<PnSignDocumentResponse> response = pnSignProviderService.signPdfDocument(fileBytes, true);
        StepVerifier.create(response).expectError(MaxRetryExceededException.class).verify();

        verify(namirialSignProviderService, times(1)).signPdfDocument(fileBytes, true);
        verify(arubaSignProviderService, never()).signPdfDocument(any(), anyBoolean());
        verify(dummySignProviderService, never()).signPdfDocument(any(), anyBoolean());
    }

    @Test
    void namirialProvider_signPdf_Perm() {
        ReflectionTestUtils.setField(pnSignServiceConfigurationProperties, PROVIDER_SWITCH, CONDITIONAL_DATE_PROVIDER_FUTURE);

        mockAltProvPdfSignatureV2Async(namirialSignProviderService,  RESP_PERM);

        Mono<PnSignDocumentResponse> response = pnSignProviderService.signPdfDocument(fileBytes, true);
        StepVerifier.create(response).expectError(PnSpapiPermanentErrorException.class).verify();

        verify(namirialSignProviderService, times(1)).signPdfDocument(fileBytes, true);
        verify(arubaSignProviderService, never()).signPdfDocument(any(), anyBoolean());
        verify(dummySignProviderService, never()).signPdfDocument(any(), anyBoolean());
    }

    @Test
    void namirialProvider_signXml_ok() {
        ReflectionTestUtils.setField(pnSignServiceConfigurationProperties, PROVIDER_SWITCH, CONDITIONAL_DATE_PROVIDER_FUTURE);

        mockAltProvXmlSignatureAsync(namirialSignProviderService,  RESP_OK);

        Mono<PnSignDocumentResponse> response = pnSignProviderService.signXmlDocument(fileBytes, true);
        StepVerifier.create(response).expectNextCount(1).verifyComplete();

        verify(namirialSignProviderService, times(1)).signXmlDocument(fileBytes, true);
        verify(arubaSignProviderService, never()).signXmlDocument(any(), anyBoolean());
        verify(dummySignProviderService, never()).signXmlDocument(any(), anyBoolean());
        verify(cloudWatchMetricsService, times(1)).publishResponseTime(eq(namirialNamespace), eq(XADES.getValue()), anyLong(), anyLong());
    }

    @Test
    void namirialProvider_signXml_Temp() {
        ReflectionTestUtils.setField(pnSignServiceConfigurationProperties, PROVIDER_SWITCH, CONDITIONAL_DATE_PROVIDER_FUTURE);

        mockAltProvXmlSignatureAsync(namirialSignProviderService, RESP_TEMP);

        Mono<PnSignDocumentResponse> response = pnSignProviderService.signXmlDocument(fileBytes, true);
        StepVerifier.create(response).expectError(MaxRetryExceededException.class).verify();

        verify(namirialSignProviderService, times(1)).signXmlDocument(fileBytes, true);
        verify(arubaSignProviderService, never()).signXmlDocument(any(), anyBoolean());
        verify(dummySignProviderService, never()).signXmlDocument(any(), anyBoolean());
    }

    @Test
    void namirialProvider_signXml_Perm() {
        ReflectionTestUtils.setField(pnSignServiceConfigurationProperties, PROVIDER_SWITCH, CONDITIONAL_DATE_PROVIDER_FUTURE);

        mockAltProvXmlSignatureAsync(namirialSignProviderService, RESP_PERM);

        Mono<PnSignDocumentResponse> response = pnSignProviderService.signXmlDocument(fileBytes, true);
        StepVerifier.create(response).expectError(PnSpapiPermanentErrorException.class).verify();

        verify(namirialSignProviderService, times(1)).signXmlDocument(fileBytes, true);
        verify(arubaSignProviderService, never()).signXmlDocument(any(), anyBoolean());
        verify(dummySignProviderService, never()).signXmlDocument(any(), anyBoolean());
    }

    @Test
    void namirialProvider_pkcs7Signature_ok() {
        ReflectionTestUtils.setField(pnSignServiceConfigurationProperties, PROVIDER_SWITCH, CONDITIONAL_DATE_PROVIDER_FUTURE);

        mockAltProvPkcs7SignV2Async(namirialSignProviderService, RESP_OK);

        Mono<PnSignDocumentResponse> response = pnSignProviderService.pkcs7Signature(fileBytes, true);
        StepVerifier.create(response).expectNextCount(1).verifyComplete();

        verify(namirialSignProviderService, times(1)).pkcs7Signature(fileBytes, true);
        verify(arubaSignProviderService, never()).pkcs7Signature(any(), anyBoolean());
        verify(dummySignProviderService, never()).pkcs7Signature(any(), anyBoolean());
        verify(cloudWatchMetricsService, times(1)).publishResponseTime(eq(namirialNamespace), eq(CADES.getValue()), anyLong(), anyLong());
    }

    @Test
    void namirialProvider_pkcs7Signature_Temp() {
        ReflectionTestUtils.setField(pnSignServiceConfigurationProperties, PROVIDER_SWITCH, CONDITIONAL_DATE_PROVIDER_FUTURE);

        mockAltProvPkcs7SignV2Async(namirialSignProviderService, RESP_TEMP);

        Mono<PnSignDocumentResponse> response = pnSignProviderService.pkcs7Signature(fileBytes, true);
        StepVerifier.create(response).expectError(MaxRetryExceededException.class).verify();

        verify(namirialSignProviderService, times(1)).pkcs7Signature(fileBytes, true);
        verify(arubaSignProviderService, never()).pkcs7Signature(any(), anyBoolean());
        verify(dummySignProviderService, never()).pkcs7Signature(any(), anyBoolean());
    }

    @Test
    void namirialProvider_pkcs7Signature_Perm() {
        ReflectionTestUtils.setField(pnSignServiceConfigurationProperties, PROVIDER_SWITCH, CONDITIONAL_DATE_PROVIDER_FUTURE);

        mockAltProvPkcs7SignV2Async(namirialSignProviderService, RESP_PERM);

        Mono<PnSignDocumentResponse> response = pnSignProviderService.pkcs7Signature(fileBytes, true);
        StepVerifier.create(response).expectError(PnSpapiPermanentErrorException.class).verify();

        verify(namirialSignProviderService, times(1)).pkcs7Signature(fileBytes, true);
        verify(arubaSignProviderService, never()).pkcs7Signature(any(), anyBoolean());
        verify(dummySignProviderService, never()).pkcs7Signature(any(), anyBoolean());
    }

    @Test
    void conditionalDateFutureProvider_signPdf_ok() {
        ReflectionTestUtils.setField(pnSignServiceConfigurationProperties, PROVIDER_SWITCH, CONDITIONAL_DATE_PROVIDER_FUTURE);

        mockAltProvPdfSignatureV2Async(namirialSignProviderService,RESP_OK);

        Mono<PnSignDocumentResponse> response = pnSignProviderService.signPdfDocument(fileBytes, true);
        StepVerifier.create(response).expectNextCount(1).verifyComplete();

        verify(namirialSignProviderService, times(1)).signPdfDocument(fileBytes, true);
        verify(arubaSignProviderService, never()).signPdfDocument(any(), anyBoolean());
        verify(dummySignProviderService, never()).signPdfDocument(any(), anyBoolean());
    }

    @Test
    void conditionalDatePastProvider_signPdf_ok() {
        ReflectionTestUtils.setField(pnSignServiceConfigurationProperties, PROVIDER_SWITCH, CONDITIONAL_DATE_PROVIDER_PAST);

        mockArubaPdfSignatureV2Async(arubaSignServiceClient, "ok", fileBytes, RESP_OK);

        Mono<PnSignDocumentResponse> response = pnSignProviderService.signPdfDocument(fileBytes, true);
        StepVerifier.create(response).expectNextCount(1).verifyComplete();

        verify(arubaSignProviderService, times(1)).signPdfDocument(fileBytes, true);
        verify(namirialSignProviderService, never()).signPdfDocument(any(), anyBoolean());
        verify(dummySignProviderService, never()).signPdfDocument(any(), anyBoolean());
    }

    @Test
    void setConditionalDateProviderDummy_signPdf_ok(){
        ReflectionTestUtils.setField(pnSignServiceConfigurationProperties, PROVIDER_SWITCH, CONDITIONAL_DATE_PROVIDER_DUMMY);

        Mono<PnSignDocumentResponse> response = pnSignProviderService.signPdfDocument(fileBytes, true);
        StepVerifier.create(response).expectNextMatches(r -> r.getSignedDocument() == fileBytes).verifyComplete();

        verify(dummySignProviderService, times(1)).signPdfDocument(fileBytes, true);
        verify(arubaSignProviderService, never()).signPdfDocument(any(), anyBoolean());
        verify(namirialSignProviderService, never()).signPdfDocument(any(), anyBoolean());
    }

    @Test
    void setConditionalDateProviderDummy_signXml_ok(){
        ReflectionTestUtils.setField(pnSignServiceConfigurationProperties, PROVIDER_SWITCH, CONDITIONAL_DATE_PROVIDER_DUMMY);

        Mono<PnSignDocumentResponse> response = pnSignProviderService.signXmlDocument(fileBytes, true);
        StepVerifier.create(response).expectNextMatches(r -> r.getSignedDocument() == fileBytes).verifyComplete();

        verify(dummySignProviderService, times(1)).signXmlDocument(fileBytes, true);
        verify(arubaSignProviderService, never()).signXmlDocument(any(), anyBoolean());
        verify(namirialSignProviderService, never()).signXmlDocument(any(), anyBoolean());
    }

    @Test
    void setConditionalDateProviderDummy_pkcs7Signature_ok(){
        ReflectionTestUtils.setField(pnSignServiceConfigurationProperties, PROVIDER_SWITCH, CONDITIONAL_DATE_PROVIDER_DUMMY);

        Mono<PnSignDocumentResponse> response = pnSignProviderService.pkcs7Signature(fileBytes, true);
        StepVerifier.create(response).expectNextMatches(r -> r.getSignedDocument() == fileBytes).verifyComplete();

        verify(dummySignProviderService, times(1)).pkcs7Signature(fileBytes, true);
        verify(arubaSignProviderService, never()).pkcs7Signature(any(), anyBoolean());
        verify(namirialSignProviderService, never()).pkcs7Signature(any(), anyBoolean());
    }

}
