package it.pagopa.pn.ms.be.service.sign;

import it.pagopa.pn.ms.be.service.sign.wsdl.ArubaSignServiceService;
import it.pagopa.pn.ms.be.service.sign.wsdl.Auth;

public abstract class CommonArubaService {


    ArubaSignServiceService arubaSignService = new ArubaSignServiceService();


    Auth identity;

    public static  String delegated_domain="demoprod";
    public static  String delegated_password="password11";
    public static  String delegated_user="delegato";
    public static  String otpPwd="dsign";
    public static  String typeOtpAuth="demoprod";
    public static  String user="titolare_aut";

    public Auth createIdentity (){
        Auth auth = new Auth();
        auth.setDelegatedDomain(delegated_domain);
        auth.setDelegatedPassword(delegated_password);
        auth.setDelegatedUser(delegated_user);
        auth.setOtpPwd(otpPwd);
        auth.setTypeOtpAuth(typeOtpAuth);
        auth.setUser(user);

        return  auth;

    }

}
