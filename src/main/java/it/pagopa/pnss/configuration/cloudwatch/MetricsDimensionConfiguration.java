package it.pagopa.pnss.configuration.cloudwatch;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pnss.common.utils.MetricsDimensionParser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.services.cloudwatch.model.Dimension;
import software.amazon.awssdk.services.ssm.SsmClient;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.*;

/**
 * A configuration class for metrics dimensions. It prepares objects to handle metrics dimensions.
 */
@Configuration
public class MetricsDimensionConfiguration {

    private final SsmClient ssmClient;
    private final MetricsDimensionParser metricsDimensionParser;
    private Map<String, Map<String, List<Long>>> dimensionsSchema = new HashMap<>();
    @Value("${pn.sign.cloudwatch.metric.dimension.file-size-range}")
    private String fileSizeRangeDimensionName;
    @Value("${pn.sign.dimension.metrics.schema}")
    private String signMetricsDimensionSchema;

    public MetricsDimensionConfiguration(SsmClient ssmClient, MetricsDimensionParser metricsDimensionParser) {
        this.ssmClient = ssmClient;
        this.metricsDimensionParser = metricsDimensionParser;
    }

    /**
     * Init method to initialize the metrics dimensions schema. It gets the schema from the SSM parameter store.
     * The schema is a JSON string that contains the dimensions for each type of signature, divided by file size range.
     */
    @PostConstruct
    public void init() {
        String dimensionsJsonSchema = ssmClient.getParameter(builder -> builder.name(signMetricsDimensionSchema)).parameter().value();
        dimensionsSchema = metricsDimensionParser.parseSignDimensionJson(dimensionsJsonSchema);
    }

    /**
     * Gets the dimension for a given signature type and file size.
     *
     * @param signType the signature type
     * @param fileSize the file size
     * @return the dimension
     */
    public Dimension getDimension(String signType, Long fileSize) {
        Optional<Dimension> dimensionOpt = dimensionsSchema.get(signType).entrySet().stream()
                .filter(entry -> {
                    List<Long> responseTimeRange = entry.getValue();
                    // if the range has only one element, it means that it is the last available range
                    if (responseTimeRange.size() == 1) {
                        return fileSize >= entry.getValue().get(0);
                    } else return fileSize >= entry.getValue().get(0) && fileSize < entry.getValue().get(1);
                })
                .map(entry -> Dimension.builder().name(fileSizeRangeDimensionName).value(entry.getKey()).build())
                .findFirst();

        if (dimensionOpt.isEmpty()) {
            throw new IllegalArgumentException("Dimension not found for signType: " + signType + " fileSize: " + fileSize);
        } else return dimensionOpt.get();

    }

}
