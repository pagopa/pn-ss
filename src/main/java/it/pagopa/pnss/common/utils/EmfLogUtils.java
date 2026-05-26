package it.pagopa.pnss.common.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.CustomLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@CustomLog
public class EmfLogUtils {

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final Logger jsonLogger = LoggerFactory.getLogger("it.pagopa.pnss.JsonLogger");

    private static final String AWS = "_aws";
    private static final String TIMESTAMP = "Timestamp";
    private static final String CLOUDWATCH_METRICS = "CloudWatchMetrics";
    private static final String NAMESPACE = "Namespace";
    private static final String METRICS = "Metrics";
    private static final String DIMENSIONS = "Dimensions";
    private static final String NAME = "Name";
    private static final String UNIT = "Unit";
    private static final String UNIT_COUNT = "Count";
    private static final String SERVICE = "Service";
    private static final String SERVICE_SAFE_STORAGE = "SafeStorage";

    public static final String NAMESPACE_PN_SAFE_STORAGE = "PN-SafeStorage";
    public static final String METRIC_CADES_CHECKSUM_MISMATCH_EXHAUSTED = "CadesChecksumMismatchExhausted";

    private EmfLogUtils() {
        throw new IllegalStateException("EmfLogUtils is a utility class");
    }

    public static void trackCadesChecksumMismatchExhausted(String fileKey) {
        try {
            String emfLog = createEmfLog(
                    NAMESPACE_PN_SAFE_STORAGE,
                    METRIC_CADES_CHECKSUM_MISMATCH_EXHAUSTED,
                    UNIT_COUNT,
                    List.of(SERVICE),
                    Map.of(METRIC_CADES_CHECKSUM_MISMATCH_EXHAUSTED, 1, SERVICE, SERVICE_SAFE_STORAGE, "FileKey", fileKey)
            );
            jsonLogger.info(emfLog);
        } catch (Exception e) {
            log.warn("Failed to emit EMF log for CAdES checksum mismatch exhausted", e);
        }
    }

    private static String createEmfLog(String namespace, String metricName, String unit, List<String> dimensions, Map<String, Object> values) {
        try {
            ObjectNode root = objectMapper.createObjectNode();

            ObjectNode awsNode = objectMapper.createObjectNode();
            awsNode.put(TIMESTAMP, Instant.now().toEpochMilli());

            ObjectNode metricsNode = objectMapper.createObjectNode();
            metricsNode.put(NAMESPACE, namespace);

            ObjectNode metricDef = objectMapper.createObjectNode();
            metricDef.put(NAME, metricName);
            metricDef.put(UNIT, unit);

            ArrayNode dimensionsArray = objectMapper.createArrayNode();
            if (dimensions != null) {
                dimensions.forEach(dimensionsArray::add);
            }

            metricsNode.set(METRICS, objectMapper.createArrayNode().add(metricDef));
            metricsNode.set(DIMENSIONS, objectMapper.createArrayNode().add(dimensionsArray));

            awsNode.set(CLOUDWATCH_METRICS, objectMapper.createArrayNode().add(metricsNode));
            root.set(AWS, awsNode);

            if (values != null) {
                values.forEach(root::putPOJO);
            }

            return objectMapper.writeValueAsString(root);
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to create EMF JSON log", e);
        }
    }
}
