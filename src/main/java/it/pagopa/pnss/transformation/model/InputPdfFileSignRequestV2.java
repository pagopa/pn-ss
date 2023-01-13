package it.pagopa.pnss.transformation.model;

import it.pagopa.pnss.transformation.wsdl.SignRequestV2;

public class InputPdfFileSignRequestV2 {

    SignRequestV2 infoTosigned;
    String url ;

    public SignRequestV2 getInfoTosigned() {
        return infoTosigned;
    }

    public void setInfoTosigned(SignRequestV2 infoTosigned) {
        this.infoTosigned = infoTosigned;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
