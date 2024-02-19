package it.pagopa.pn.library.sign.service.impl;

import it.pagopa.pn.library.sign.configurationproperties.PnSignServiceConfigurationProperties;
import it.pagopa.pn.library.sign.pojo.PnSignDocumentResponse;
import it.pagopa.pn.library.sign.service.IPnSignService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service("pnSignService")
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
        return getProvider(pnSignServiceConfigurationProperties.getProviderSwitch()).signPdfDocument(fileBytes, timestamping);
    }

    @Override
    public Mono<PnSignDocumentResponse> signXmlDocument(byte[] fileBytes, Boolean timestamping) {
        return getProvider(pnSignServiceConfigurationProperties.getProviderSwitch()).signXmlDocument(fileBytes, timestamping);
    }

    @Override
    public Mono<PnSignDocumentResponse> pkcs7Signature(byte[] fileBytes, Boolean timestamping) {
        return getProvider(pnSignServiceConfigurationProperties.getProviderSwitch()).pkcs7Signature(fileBytes, timestamping);
    }

    private IPnSignService getProvider(String providerName) {
        if (providerName.equals("aruba"))
            return arubaSignProviderService;
        else return alternativeSignProviderService;
    }

}
