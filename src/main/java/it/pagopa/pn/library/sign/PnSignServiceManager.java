package it.pagopa.pn.library.sign;


import com.namirial.sign.library.service.PnSignServiceImpl;
import it.pagopa.pn.library.sign.service.PnSignService;
import it.pagopa.pn.library.sign.service.impl.ArubaSignProviderService;
import it.pagopa.pn.ss.dummy.sign.service.PnDummySignServiceImpl;
import lombok.CustomLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

import static it.pagopa.pnss.common.constant.Constant.*;

@Component
@CustomLog
@DependsOn("pnSignCredentialConf")
public class PnSignServiceManager {
   ArubaSignProviderService arubaSignProviderService;
   PnSignServiceImpl namirialProviderService;
   PnDummySignServiceImpl dummyProviderService;

    @Autowired
    public PnSignServiceManager(ArubaSignProviderService arubaSignProviderService, PnSignServiceImpl namirialSignProviderService,PnDummySignServiceImpl dummyProviderService){
        this.arubaSignProviderService = arubaSignProviderService;
        this.namirialProviderService = namirialSignProviderService;
        this.dummyProviderService = dummyProviderService;
    }

    public PnSignService getProviderService(String providerSwitch) {
        return switch (providerSwitch) {
            case ARUBA -> arubaSignProviderService;
            case NAMIRIAL -> namirialProviderService;
            case DUMMY -> dummyProviderService;
            default -> throw new IllegalArgumentException("Invalid provider: " + providerSwitch);
        };
    }
}
