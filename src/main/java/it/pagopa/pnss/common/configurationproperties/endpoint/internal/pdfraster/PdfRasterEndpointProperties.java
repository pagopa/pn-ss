package it.pagopa.pnss.common.configurationproperties.endpoint.internal.pdfraster;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "internal-endpoint.pdf-raster")
public record PdfRasterEndpointProperties(String containerBaseUrl, String convertPdf) {
}
