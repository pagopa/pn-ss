package it.pagopa.pnss.common.rest.call.pdfraster;

import reactor.core.publisher.Mono;

public interface PdfRasterCall {

    Mono<byte[]> convertPdf(byte[] fileBytes);

}
