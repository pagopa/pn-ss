package it.pagopa.pnss.transformation.model;

import it.pagopa.pnss.transformation.wsdl.SignReturnV2;

import java.util.Objects;

public class PdfFileSignReturnV2 extends ErrorResponse {
    SignReturnV2 pdfInfoResultSign;

    public SignReturnV2 getPdfInfoResultSign() {
        return pdfInfoResultSign;
    }

    public void setPdfInfoResultSign(SignReturnV2 pdfInfoResultSign) {
        this.pdfInfoResultSign = pdfInfoResultSign;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        PdfFileSignReturnV2 that = (PdfFileSignReturnV2) o;
        return Objects.equals(pdfInfoResultSign, that.pdfInfoResultSign);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), pdfInfoResultSign);
    }

}
