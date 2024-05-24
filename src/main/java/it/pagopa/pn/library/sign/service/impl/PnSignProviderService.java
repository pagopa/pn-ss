package it.pagopa.pn.library.sign.service.impl;

import com.namirial.sign.library.service.PnSignServiceImpl;
import it.pagopa.pn.library.exceptions.PnSpapiTemporaryErrorException;
import it.pagopa.pn.library.sign.configurationproperties.PnSignRetryStrategyProperties;
import it.pagopa.pn.library.sign.configurationproperties.PnSignServiceConfigurationProperties;
import it.pagopa.pn.library.sign.exception.MaxRetryExceededException;
import it.pagopa.pn.library.sign.pojo.PnSignDocumentResponse;
import it.pagopa.pn.library.sign.service.PnSignService;
import it.pagopa.pn.library.sign.PnSignServiceManager;
import it.pagopa.pnss.common.service.impl.CloudWatchMetricsService;
import lombok.CustomLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;

import static it.pagopa.pn.library.sign.pojo.SignatureType.*;
import static it.pagopa.pnss.common.utils.LogUtils.*;

@Service("pnSignService")
@CustomLog
public class PnSignProviderService implements PnSignService {

    private PnSignServiceManager pnSignServiceManager;
    private final PnSignServiceConfigurationProperties pnSignServiceConfigurationProperties;
    private final Retry pnSignRetryStrategy;
    private final CloudWatchMetricsService cloudWatchMetricsService;
    @Value("${pn.sign.cloudwatch.namespace.aruba}")
    private String arubaNamespace;
    @Value("${pn.sign.cloudwatch.namespace.namirial}")
    private String namirialNamespace;
    @Value("${pn.sign.cloudwatch.metric.response-time.pades}")
    private String signPadesReadResponseTimeMetric;
    @Value("${pn.sign.cloudwatch.metric.response-time.xades}")
    private String signXadesReadResponseTimeMetric;
    @Value("${pn.sign.cloudwatch.metric.response-time.cades}")
    private String signCadesReadResponseTimeMetric;

    @Autowired
    public PnSignProviderService(PnSignServiceConfigurationProperties pnSignServiceConfigurationProperties, PnSignRetryStrategyProperties pnSignRetryStrategyProperties, PnSignServiceManager pnSignServiceManager, CloudWatchMetricsService cloudWatchMetricsService) {
        this.pnSignServiceConfigurationProperties = pnSignServiceConfigurationProperties;
        this.pnSignServiceManager = pnSignServiceManager;
        this.cloudWatchMetricsService = cloudWatchMetricsService;
        this.pnSignRetryStrategy = Retry.backoff(pnSignRetryStrategyProperties.maxAttempts(), Duration.ofSeconds(pnSignRetryStrategyProperties.minBackoff()))
                .filter(PnSpapiTemporaryErrorException.class::isInstance)
                .doBeforeRetry(retrySignal -> log.warn(RETRY_ATTEMPT, retrySignal.totalRetries(), retrySignal.failure().getMessage()))
                .onRetryExhaustedThrow((retrySpec, retrySignal) -> new MaxRetryExceededException("Maximum retries exceeded"));
    }

    @Override
    public Mono<PnSignDocumentResponse> signPdfDocument(byte[] fileBytes, Boolean timestamping) {
        log.debug(INVOKING_METHOD, PN_SIGN_PDF_DOCUMENT, timestamping);
        PnSignService provider = getProvider(pnSignServiceConfigurationProperties.getProviderSwitch());
        return cloudWatchMetricsService.executeAndPublishResponseTime(provider.signPdfDocument(fileBytes, timestamping), getMetricNamespace(provider), PADES)
                .retryWhen(pnSignRetryStrategy)
                .doOnSuccess(result -> log.info(SUCCESSFUL_OPERATION_LABEL, PN_SIGN_PDF_DOCUMENT, result));
    }

    @Override
    public Mono<PnSignDocumentResponse> signXmlDocument(byte[] fileBytes, Boolean timestamping)  {
        log.debug(INVOKING_METHOD, PN_SIGN_XML_DOCUMENT, timestamping);
        PnSignService provider = getProvider(pnSignServiceConfigurationProperties.getProviderSwitch());
        return cloudWatchMetricsService.executeAndPublishResponseTime(provider.signXmlDocument(fileBytes, timestamping), getMetricNamespace(provider), XADES)
                .retryWhen(pnSignRetryStrategy)
                .doOnSuccess(result -> log.info(SUCCESSFUL_OPERATION_LABEL, PN_SIGN_XML_DOCUMENT, result));
    }

    @Override
    public Mono<PnSignDocumentResponse> pkcs7Signature(byte[] fileBytes, Boolean timestamping)  {
        log.debug(INVOKING_METHOD, PN_PKCS_7_SIGNATURE, timestamping);
        PnSignService provider = getProvider(pnSignServiceConfigurationProperties.getProviderSwitch());
        return cloudWatchMetricsService.executeAndPublishResponseTime(provider.pkcs7Signature(fileBytes, timestamping), getMetricNamespace(provider), CADES)
                .retryWhen(pnSignRetryStrategy)
                .doOnSuccess(result -> log.info(SUCCESSFUL_OPERATION_LABEL, PN_PKCS_7_SIGNATURE, result));
    }

    private PnSignService getProvider(String providerName) {
        if (providerName.equals("aruba"))
            return pnSignServiceManager.getArubaSignProviderService();
        else return pnSignServiceManager.getNamirialProviderService();
    }

    /**
     * Method to get the metric namespace based on the signature provider
     *
     * @param service the specific provider service
     * @return the metric namespace
     */
    private String getMetricNamespace(PnSignService service) {
        if (service instanceof ArubaSignProviderService) {
            return arubaNamespace;
        } else if (service instanceof PnSignServiceImpl) {
            return namirialNamespace;
        } else {
            log.debug(ERROR_RETRIEVING_METRIC_NAMESPACE);
            throw new IllegalArgumentException(ERROR_RETRIEVING_METRIC_NAMESPACE);
        }
    }

}
