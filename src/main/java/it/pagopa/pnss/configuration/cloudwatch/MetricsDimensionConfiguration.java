package it.pagopa.pnss.configuration.cloudwatch;

import it.pagopa.pnss.common.exception.CloudWatchResourceNotFoundException;
import it.pagopa.pnss.common.utils.MetricsDimensionParser;
import lombok.CustomLog;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.services.cloudwatch.model.Dimension;
import software.amazon.awssdk.services.ssm.SsmClient;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.stream.Stream;

import static it.pagopa.pnss.common.utils.LogUtils.*;

/**
 * A configuration class for metrics dimensions. It prepares objects to handle metrics dimensions.
 */
@Configuration
@CustomLog
public class MetricsDimensionConfiguration {

    private final SsmClient ssmClient;
    private final MetricsDimensionParser metricsDimensionParser;
    private Map<String, Map<String, List<Long>>> dimensionsSchema = new HashMap<>();
    @Value("${pn.sign.cloudwatch.metric.dimension.file-size-range}")
    private String fileSizeRangeDimensionName;
    @Value("${pn.sign.dimension.metrics.schema}")
    private String signMetricsDimensionSchema;

    public MetricsDimensionConfiguration(SsmClient ssmClient) {
        this.ssmClient = ssmClient;
        this.metricsDimensionParser = new MetricsDimensionParser();
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
        log.debug(INVOKING_METHOD, GET_DIMENSION, Stream.of(signType, fileSize).toList());
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
            // if the file size is not included in any range, the dimension is not found, so we throw an exception.
            throw new CloudWatchResourceNotFoundException.DimensionNotFound(signType, fileSize);
        } else return dimensionOpt.get();

    }

}
