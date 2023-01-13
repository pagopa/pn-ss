package it.pagopa.pnss.transformation.model;

import it.pagopa.pnss.transformation.wsdl.SignReturnV2;

public class PdfFileSignReturnV2 extends ErrorResponse{
    SignReturnV2 pdfInfoResultSign;

    public SignReturnV2 getPdfInfoResultSign() {
        return pdfInfoResultSign;
    }

    public void setPdfInfoResultSign(SignReturnV2 pdfInfoResultSign) {
        this.pdfInfoResultSign = pdfInfoResultSign;
    }
}
