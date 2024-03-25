package it.pagopa.pn.library.sign;


import it.pagopa.pn.library.sign.service.PnSignService;
import it.pagopa.pn.library.sign.service.impl.AlternativeSignProviderService;
import it.pagopa.pn.library.sign.service.impl.ArubaSignProviderService;
import lombok.CustomLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

@Component
@CustomLog
@DependsOn("pnSignCredentialConf")
public class PnSignServiceManager {
   ArubaSignProviderService arubaSignProviderService;
   AlternativeSignProviderService alternativeProviderService;

    @Autowired
    public PnSignServiceManager(ArubaSignProviderService arubaSignProviderService, AlternativeSignProviderService alternativeProviderService) {
        this.arubaSignProviderService = arubaSignProviderService;
        this.alternativeProviderService = alternativeProviderService;
    }

    public PnSignService getArubaSignProviderService() {
        return arubaSignProviderService;
    }

    public PnSignService getAlternativeProviderService() {
        return alternativeProviderService;
    }

}
