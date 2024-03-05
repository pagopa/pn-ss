package it.pagopa.pn.library.sign.service.impl;

import it.pagopa.pn.library.sign.configurationproperties.PnSignServiceConfigurationProperties;
import it.pagopa.pn.library.sign.pojo.PnSignDocumentResponse;
import it.pagopa.pn.library.sign.service.IPnSignService;
import lombok.CustomLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import static it.pagopa.pnss.common.utils.LogUtils.*;

@Service("pnSignService")
@CustomLog
public class PnSignProviderService implements IPnSignService {

    private final ArubaSignProviderService arubaSignProviderService;
    private final AlternativeSignProviderService alternativeSignProviderService;
    private final PnSignServiceConfigurationProperties pnSignServiceConfigurationProperties;

    @Autowired
    public PnSignProviderService(ArubaSignProviderService arubaSignProviderService, AlternativeSignProviderService alternativeSignProviderService, PnSignServiceConfigurationProperties pnSignServiceConfigurationProperties) {
        this.arubaSignProviderService = arubaSignProviderService;
        this.alternativeSignProviderService = alternativeSignProviderService;
        this.pnSignServiceConfigurationProperties = pnSignServiceConfigurationProperties;
    }

    @Override
    public Mono<PnSignDocumentResponse> signPdfDocument(byte[] fileBytes, Boolean timestamping) {
        log.debug(INVOKING_METHOD, PN_SIGN_PDF_DOCUMENT, timestamping);
        return getProvider(pnSignServiceConfigurationProperties.getProviderSwitch()).signPdfDocument(fileBytes, timestamping)
                .doOnSuccess(result -> log.info(SUCCESSFUL_OPERATION_LABEL, PN_SIGN_PDF_DOCUMENT, result));
    }

    @Override
    public Mono<PnSignDocumentResponse> signXmlDocument(byte[] fileBytes, Boolean timestamping) {
        log.debug(INVOKING_METHOD, PN_SIGN_XML_DOCUMENT, timestamping);
        return getProvider(pnSignServiceConfigurationProperties.getProviderSwitch()).signXmlDocument(fileBytes, timestamping)
                .doOnSuccess(result -> log.info(SUCCESSFUL_OPERATION_LABEL, PN_SIGN_XML_DOCUMENT, result));
    }

    @Override
    public Mono<PnSignDocumentResponse> pkcs7Signature(byte[] fileBytes, Boolean timestamping) {
        log.debug(INVOKING_METHOD, PN_PKCS_7_SIGNATURE, timestamping);
        return getProvider(pnSignServiceConfigurationProperties.getProviderSwitch()).pkcs7Signature(fileBytes, timestamping)
                .doOnSuccess(result -> log.info(SUCCESSFUL_OPERATION_LABEL, PN_PKCS_7_SIGNATURE, result));
    }

    private IPnSignService getProvider(String providerName) {
        if (providerName.equals("aruba"))
            return arubaSignProviderService;
        else return alternativeSignProviderService;
    }

}
