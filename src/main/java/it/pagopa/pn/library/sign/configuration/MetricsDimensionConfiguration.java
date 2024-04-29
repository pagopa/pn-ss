package it.pagopa.pn.library.sign.configuration;

import it.pagopa.pn.library.sign.utils.MetricsDimensionParser;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.services.cloudwatch.model.Dimension;
import software.amazon.awssdk.services.ssm.SsmClient;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Configuration
public class MetricsDimensionConfiguration {

    private final SsmClient ssmClient;
    private final MetricsDimensionParser metricsDimensionParser;

    private Map<String, Map<String, List<Long>>> dimensionsSchema = new HashMap<>();

    public MetricsDimensionConfiguration(SsmClient ssmClient, MetricsDimensionParser metricsDimensionParser) {
        this.ssmClient = ssmClient;
        this.metricsDimensionParser = metricsDimensionParser;
    }

    @PostConstruct
    public void init() {
        String dimensionsJsonSchema = ssmClient.getParameter(builder -> builder.name("pn-SS-sign-dimensions-schema")).parameter().value();
        dimensionsSchema = metricsDimensionParser.parseSignDimensionJson(dimensionsJsonSchema);
    }

    public Dimension getDimension(String signType, Long fileSize, Long responseTime) {
        return Dimension.builder().build();
    }

}
