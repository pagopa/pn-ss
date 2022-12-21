package it.pagopa.pn.ms.be.service.sign;

import it.pagopa.pn.ms.be.service.sign.dto.GenericFileSignRequestV2;
import it.pagopa.pn.ms.be.service.sign.dto.GenericFileSignReturnV2;
import it.pagopa.pn.ms.be.service.sign.dto.InputPdfFileSignRequestV2;
import it.pagopa.pn.ms.be.service.sign.dto.PdfFileSignReturnV2;
import it.pagopa.pn.ms.be.service.sign.wsdl.SignReturnV2;
import org.springframework.beans.factory.annotation.Autowired;

public class ServiceSignImpl {

    @Autowired
    SignServiceSoap signServiceSoap;

    public PdfFileSignReturnV2 singnPdfDocument(InputPdfFileSignRequestV2 input ){

        return signServiceSoap.callArubaSignPdfFile(input);
    }






    public GenericFileSignReturnV2 pkcs7signV2(GenericFileSignRequestV2 input){

        return signServiceSoap.callGenericFile(input);
    }


}
