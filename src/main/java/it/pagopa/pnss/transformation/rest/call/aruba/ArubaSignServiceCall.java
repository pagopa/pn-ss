package it.pagopa.pnss.transformation.rest.call.aruba;

import it.pagopa.pnss.transformation.wsdl.SignReturnV2;
import reactor.core.publisher.Mono;

public interface ArubaSignServiceCall {

    Mono<SignReturnV2> signPdfDocument(byte[] pdfFile, Boolean marcatura);
    Mono<SignReturnV2> pkcs7signV2(byte[] buf, Boolean marcatura);
    Mono<SignReturnV2> xmlSignature(byte[] xmlBytes, Boolean marcatura);
}
