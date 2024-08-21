package it.pagopa.pnss.common.rest.call.pdfraster;

import it.pagopa.pnss.common.configurationproperties.endpoint.internal.pdfraster.PdfRasterEndpointProperties;
import it.pagopa.pnss.configurationproperties.PdfRasterRetryStrategyProperties;
import lombok.CustomLog;
import lombok.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.http.codec.multipart.Part;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
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

    public Mono<byte[]> convertPdf(byte[] fileBytes, String fileKey) {
        log.logInvokingExternalService("pn-pdfraster", "convertPdf()");
        MultiValueMap<String, Object> multipartData = new LinkedMultiValueMap<>();
        multipartData.add("file", new ByteArrayResource(fileBytes){
            @Override
            public String getFilename() {
                return fileKey;
            }
        });
        return pdfRasterWebClient.post()
                .uri(pdfRasterEndpointProperties.convertPdf())
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(multipartData))
                .retrieve()
                .bodyToMono(byte[].class)
                .retryWhen(pdfRasterRetryStrategy);
    }

}
