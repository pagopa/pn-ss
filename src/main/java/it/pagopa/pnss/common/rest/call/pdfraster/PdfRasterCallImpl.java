package it.pagopa.pnss.common.rest.call.pdfraster;

import it.pagopa.pnss.common.configurationproperties.endpoint.internal.pdfraster.PdfRasterEndpointProperties;
import lombok.CustomLog;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@CustomLog
@Component
public class PdfRasterCallImpl implements PdfRasterCall {

    private final PdfRasterEndpointProperties pdfRasterEndpointProperties;
    private final WebClient pdfRasterWebClient;

    public PdfRasterCallImpl(PdfRasterEndpointProperties pdfRasterEndpointProperties, WebClient pdfRasterWebClient) {
        this.pdfRasterEndpointProperties = pdfRasterEndpointProperties;
        this.pdfRasterWebClient = pdfRasterWebClient;
    }

    public Mono<byte[]> convertPdf(byte[] fileBytes) {
        log.logInvokingExternalService("pn-pdfraster", "convertPdf()");
        return pdfRasterWebClient.post()
                .uri(pdfRasterEndpointProperties.convertPdf())
                .bodyValue(fileBytes)
                .retrieve()
                .bodyToMono(byte[].class);
    }

}
