package it.pagopa.pnss.common.rest.call.pdfraster;

import it.pagopa.pnss.common.configurationproperties.endpoint.internal.pdfraster.PdfRasterEndpointProperties;
import it.pagopa.pnss.configurationproperties.PdfRasterRetryStrategyProperties;
import lombok.CustomLog;
import lombok.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;
import reactor.util.retry.RetryBackoffSpec;

import java.time.Duration;

@CustomLog
@Component
public class PdfRasterCallImpl implements PdfRasterCall {

    private final PdfRasterEndpointProperties pdfRasterEndpointProperties;
    private final WebClient pdfRasterWebClient;
    private final RetryBackoffSpec pdfRasterRetryStrategy;

    public PdfRasterCallImpl(PdfRasterEndpointProperties pdfRasterEndpointProperties, WebClient pdfRasterWebClient, RetryBackoffSpec pdfRasterRetryStrategy) {
        this.pdfRasterEndpointProperties = pdfRasterEndpointProperties;
        this.pdfRasterWebClient = pdfRasterWebClient;
        this.pdfRasterRetryStrategy = pdfRasterRetryStrategy;
    }

    public Mono<byte[]> convertPdf(byte[] fileBytes) {
        log.logInvokingExternalService("pn-pdfraster", "convertPdf()");
        return pdfRasterWebClient.post()
                .uri(pdfRasterEndpointProperties.convertPdf())
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .bodyValue(fileBytes)
                .retrieve()
                .bodyToMono(byte[].class)
                .retryWhen(pdfRasterRetryStrategy);
    }

}
