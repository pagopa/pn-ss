package it.pagopa.pn.ms.be.service.sign.dto;

import it.pagopa.pn.ms.be.service.sign.wsdl.SignReturnV2;

public class GenericFileSignReturnV2 extends ErrorResponse {
    SignReturnV2 pdfInfoResultSign;

    public SignReturnV2 getPdfInfoResultSign() {
        return pdfInfoResultSign;
    }

    public void setPdfInfoResultSign(SignReturnV2 pdfInfoResultSign) {
        this.pdfInfoResultSign = pdfInfoResultSign;
    }
}
