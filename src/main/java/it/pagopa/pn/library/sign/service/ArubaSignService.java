package it.pagopa.pn.library.sign.service;

import it.pagopa.pnss.transformation.wsdl.SignReturnV2;
import reactor.core.publisher.Mono;

public interface ArubaSignService {

    Mono<SignReturnV2> signPdfDocument(byte[] pdfFile, Boolean marcatura);
    Mono<SignReturnV2> pkcs7signV2(byte[] buf, Boolean marcatura);
    Mono<SignReturnV2> xmlSignature(byte[] xmlBytes, Boolean marcatura);
}
