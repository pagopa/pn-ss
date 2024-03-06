package it.pagopa.pn.library.sign.service.impl;

import it.pagopa.pn.library.sign.pojo.PnSignDocumentResponse;
import it.pagopa.pn.library.sign.service.IPnSignService;
import lombok.CustomLog;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import static it.pagopa.pnss.common.utils.LogUtils.*;

@Service("alternativeProviderService")
@CustomLog
public class AlternativeSignProviderService implements IPnSignService {
    @Override
    public Mono<PnSignDocumentResponse> signPdfDocument(byte[] fileBytes, Boolean timestamping) {
        log.debug(CLIENT_METHOD_INVOCATION, ALT_SIGN_PDF_DOCUMENT, timestamping);
        return Mono.fromSupplier(() -> {
                    var response = new PnSignDocumentResponse();
                    response.setSignedDocument(new byte[0]);
                    return response;
                })
                .doOnNext(result -> log.info(CLIENT_METHOD_RETURN, ALT_SIGN_PDF_DOCUMENT, result));
    }

    @Override
    public Mono<PnSignDocumentResponse> signXmlDocument(byte[] fileBytes, Boolean timestamping) {
        log.debug(CLIENT_METHOD_INVOCATION, ALT_XML_SIGNATURE, timestamping);
        return Mono.fromSupplier(() -> {
                    var response = new PnSignDocumentResponse();
                    response.setSignedDocument(new byte[0]);
                    return response;
                })
                .doOnNext(result -> log.info(CLIENT_METHOD_RETURN, ALT_XML_SIGNATURE, result));
    }

    @Override
    public Mono<PnSignDocumentResponse> pkcs7Signature(byte[] fileBytes, Boolean timestamping) {
        log.debug(CLIENT_METHOD_INVOCATION, ALT_PKCS_7_SIGN_V2, timestamping);
        return Mono.fromSupplier(() -> {
                    var response = new PnSignDocumentResponse();
                    response.setSignedDocument(new byte[0]);
                    return response;
                })
                .doOnNext(result -> log.info(CLIENT_METHOD_RETURN, ALT_PKCS_7_SIGN_V2, result));
    }
}
