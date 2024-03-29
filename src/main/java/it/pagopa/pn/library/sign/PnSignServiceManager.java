package it.pagopa.pn.library.sign;


import com.namirial.sign.library.service.PnSignServiceImpl;
import it.pagopa.pn.library.sign.service.PnSignService;
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
   PnSignServiceImpl namirialProviderService;

    @Autowired
    public PnSignServiceManager(ArubaSignProviderService arubaSignProviderService, PnSignServiceImpl namirialSignProviderService) {
        this.arubaSignProviderService = arubaSignProviderService;
        this.namirialProviderService = namirialSignProviderService;
    }

    public PnSignService getArubaSignProviderService() {
        return arubaSignProviderService;
    }

    public PnSignService getNamirialProviderService() {
        return namirialProviderService;
    }

}
