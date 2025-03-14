package it.pagopa.pnss.configurationproperties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "pn.ss.transformation")
public class TransformationProperties {

    public  static final String TRANSFORMATION_TAG_PREFIX = "Transformation-";
    public static final String OK = "OK";
    public static final String ERROR = "ERROR";

    private long dummyDelay;
    private Queues queues;
    private MaxThreadPoolSize maxThreadPoolSize;
    private Config config;

    @Data
    public static class Config {
        private String parameterName;
    }

    @Data
    public static class Queues {
        private String signAndTimemark;
        private String sign;
        private String dummy;
    }

    @Data
    public static class MaxThreadPoolSize {
        private int signAndTimemark;
        private int sign;
    }
}
