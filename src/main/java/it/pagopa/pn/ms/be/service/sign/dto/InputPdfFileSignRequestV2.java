package it.pagopa.pn.ms.be.service.sign.dto;

import it.pagopa.pn.ms.be.service.sign.wsdl.SignRequestV2;

public class InputPdfFileSignRequestV2 {

    SignRequestV2 infoTosigned;


    public SignRequestV2 getInfoTosigned() {
        return infoTosigned;
    }

    public void setInfoTosigned(SignRequestV2 infoTosigned) {
        this.infoTosigned = infoTosigned;
    }
}
