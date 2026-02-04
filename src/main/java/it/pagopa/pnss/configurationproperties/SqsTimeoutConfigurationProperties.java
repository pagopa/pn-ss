package it.pagopa.pnss.configurationproperties;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.CustomLog;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@CustomLog
@ConfigurationProperties(prefix="pn.ss.sqs.timeout")
@Validated
@Getter
@Setter
@Component
public class SqsTimeoutConfigurationProperties {

    @Min(0)
    @Max(99)
    private int percent;
    private long defaultSeconds;
    private List<String> managedQueues;

}