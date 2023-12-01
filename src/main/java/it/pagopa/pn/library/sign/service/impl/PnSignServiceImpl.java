package it.pagopa.pn.library.sign.service.impl;

import it.pagopa.pn.library.sign.pojo.PnSignDocumentResponse;
import it.pagopa.pn.library.sign.service.ArubaSignService;
import it.pagopa.pn.library.sign.service.PnSignService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class PnSignServiceImpl implements PnSignService {
    @Autowired
    private ArubaSignService arubaSignService;

    @Override
    public Mono<PnSignDocumentResponse> signPdfDocument(byte[] fileBytes, Boolean timestamping) {
        return arubaSignService.signPdfDocument(fileBytes, timestamping).map(signReturnV2 -> {
            PnSignDocumentResponse pnSignDocumentResponse = new PnSignDocumentResponse();
            pnSignDocumentResponse.setSignedDocument(signReturnV2.getBinaryoutput());
            return pnSignDocumentResponse;
        });
    }

    @Override
    public Mono<PnSignDocumentResponse> signXmlDocument(byte[] fileBytes, Boolean timestamping) {
        return arubaSignService.xmlSignature(fileBytes, timestamping).map(signReturnV2 -> {
            PnSignDocumentResponse pnSignDocumentResponse = new PnSignDocumentResponse();
            pnSignDocumentResponse.setSignedDocument(signReturnV2.getBinaryoutput());
            return pnSignDocumentResponse;
        });
    }

    @Override
    public Mono<PnSignDocumentResponse> pkcs7Signature(byte[] fileBytes, Boolean timestamping) {
        return arubaSignService.pkcs7signV2(fileBytes, timestamping).map(signReturnV2 -> {
            PnSignDocumentResponse pnSignDocumentResponse = new PnSignDocumentResponse();
            pnSignDocumentResponse.setSignedDocument(signReturnV2.getBinaryoutput());
            return pnSignDocumentResponse;
        });
    }
}
