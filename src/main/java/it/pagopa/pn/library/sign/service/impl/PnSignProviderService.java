package it.pagopa.pn.library.sign.service.impl;

import it.pagopa.pn.library.sign.configurationproperties.PnSignRetryStrategyProperties;
import it.pagopa.pn.library.sign.configurationproperties.PnSignServiceConfigurationProperties;
import it.pagopa.pn.library.sign.exception.MaxRetryExceededException;
import it.pagopa.pn.library.sign.exception.PnSpapiTemporaryErrorException;
import it.pagopa.pn.library.sign.pojo.PnSignDocumentResponse;
import it.pagopa.pn.library.sign.service.PnSignService;
import lombok.CustomLog;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;

import static it.pagopa.pnss.common.utils.LogUtils.*;

@Service("pnSignService")
@CustomLog
public class PnSignProviderService implements PnSignService {

    private final ArubaSignProviderService arubaSignProviderService;
    private final AlternativeSignProviderService alternativeSignProviderService;
    private final PnSignServiceConfigurationProperties pnSignServiceConfigurationProperties;
    private final Retry pnSignRetryStrategy;

    @Autowired
    public PnSignProviderService(ArubaSignProviderService arubaSignProviderService, AlternativeSignProviderService alternativeSignProviderService, PnSignServiceConfigurationProperties pnSignServiceConfigurationProperties, PnSignRetryStrategyProperties pnSignRetryStrategyProperties) {
        this.arubaSignProviderService = arubaSignProviderService;
        this.alternativeSignProviderService = alternativeSignProviderService;
        this.pnSignServiceConfigurationProperties = pnSignServiceConfigurationProperties;
        this.pnSignRetryStrategy = Retry.backoff(pnSignRetryStrategyProperties.maxAttempts(), Duration.ofSeconds(pnSignRetryStrategyProperties.minBackoff()))
                .filter(PnSpapiTemporaryErrorException.class::isInstance)
                .doBeforeRetry(retrySignal -> log.warn(RETRY_ATTEMPT, retrySignal.totalRetries(), retrySignal.failure(), retrySignal.failure().getMessage()))
                .onRetryExhaustedThrow((retrySpec, retrySignal) -> new MaxRetryExceededException("Maximum retries exceeded"));
    }

    @Override
    public Mono<PnSignDocumentResponse> signPdfDocument(byte[] fileBytes, Boolean timestamping) {
        log.debug(INVOKING_METHOD, PN_SIGN_PDF_DOCUMENT, timestamping);
        return getProvider(pnSignServiceConfigurationProperties.getProviderSwitch()).signPdfDocument(fileBytes, timestamping)
                .retryWhen(pnSignRetryStrategy)
                .doOnSuccess(result -> log.info(SUCCESSFUL_OPERATION_LABEL, PN_SIGN_PDF_DOCUMENT, result));
    }

    @Override
    public Mono<PnSignDocumentResponse> signXmlDocument(byte[] fileBytes, Boolean timestamping)  {
        log.debug(INVOKING_METHOD, PN_SIGN_XML_DOCUMENT, timestamping);
        return getProvider(pnSignServiceConfigurationProperties.getProviderSwitch()).signXmlDocument(fileBytes, timestamping)
                .retryWhen(pnSignRetryStrategy)
                .doOnSuccess(result -> log.info(SUCCESSFUL_OPERATION_LABEL, PN_SIGN_XML_DOCUMENT, result));
    }

    @Override
    public Mono<PnSignDocumentResponse> pkcs7Signature(byte[] fileBytes, Boolean timestamping)  {
        log.debug(INVOKING_METHOD, PN_PKCS_7_SIGNATURE, timestamping);
        return getProvider(pnSignServiceConfigurationProperties.getProviderSwitch()).pkcs7Signature(fileBytes, timestamping)
                .retryWhen(pnSignRetryStrategy)
                .doOnSuccess(result -> log.info(SUCCESSFUL_OPERATION_LABEL, PN_PKCS_7_SIGNATURE, result));
    }

    private PnSignService getProvider(String providerName) {
        if (providerName.equals("aruba"))
            return arubaSignProviderService;
        else return alternativeSignProviderService;
    }

}
