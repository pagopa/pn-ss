package it.pagopa.pn.ms.be.service.sign;

import it.pagopa.pn.ms.be.service.sign.dto.GenericFileSignRequestV2;
import it.pagopa.pn.ms.be.service.sign.dto.GenericFileSignReturnV2;
import it.pagopa.pn.ms.be.service.sign.dto.InputPdfFileSignRequestV2;
import it.pagopa.pn.ms.be.service.sign.dto.PdfFileSignReturnV2;

public class ServiceSignImpl {

    public PdfFileSignReturnV2 singnPdfDocument(InputPdfFileSignRequestV2 input ){

        return callArubaSignPdfFile(input);
    }

    public PdfFileSignReturnV2 callArubaSignPdfFile(InputPdfFileSignRequestV2 input) {
        return new PdfFileSignReturnV2();
    }


    public GenericFileSignReturnV2 pkcs7signV2(GenericFileSignRequestV2 input){

        return callGenericFile(input);
    }

    public GenericFileSignReturnV2 callGenericFile(GenericFileSignRequestV2 input) {
        return new GenericFileSignReturnV2();
    }
}
