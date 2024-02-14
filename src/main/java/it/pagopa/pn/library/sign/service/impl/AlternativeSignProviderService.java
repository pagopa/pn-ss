package it.pagopa.pn.library.sign.service.impl;

import it.pagopa.pn.library.sign.pojo.PnSignDocumentResponse;
import it.pagopa.pn.library.sign.service.IPnSignService;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
@Service("alternativeProviderService")
public class AlternativeSignProviderService implements IPnSignService {
    @Override
    public Mono<PnSignDocumentResponse> signPdfDocument(byte[] fileBytes, Boolean timestamping) {
        return Mono.just(new PnSignDocumentResponse());
    }

    @Override
    public Mono<PnSignDocumentResponse> signXmlDocument(byte[] fileBytes, Boolean timestamping) {
        return Mono.just(new PnSignDocumentResponse());
    }

    @Override
    public Mono<PnSignDocumentResponse> pkcs7Signature(byte[] fileBytes, Boolean timestamping) {
        return Mono.just(new PnSignDocumentResponse());
    }
}
