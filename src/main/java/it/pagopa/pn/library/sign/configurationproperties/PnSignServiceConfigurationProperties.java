package it.pagopa.pn.library.sign.configurationproperties;

import lombok.CustomLog;
import lombok.Data;
import org.joda.time.DateTime;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.Pattern;

@ConfigurationProperties(prefix = "pn.sign")
@Validated
@CustomLog
@Data
public class PnSignServiceConfigurationProperties {

    @Pattern(regexp = "([A-Za-z]*)|([A-Za-z]*;\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z;[A-Za-z]*)")
    private String providerSwitch;

    private String actualProvider;

    public String getProviderSwitch() {
        return returnPropertyValue(providerSwitch);
    }

    private String returnPropertyValue(String propertyString) {
        String[] propertyArray = propertyString.split(";");
        if (propertyArray.length == 1) {
            return propertyArray[0];
        } else if (propertyArray.length == 2 || propertyArray.length > 3) {
            throw new IllegalArgumentException("Error parsing property values,wrong number of arguments.");
        } else {
            String valueBeforeDate = propertyArray[0];
            String valueAfterDate = propertyArray[2];
            DateTime date = DateTime.parse(propertyArray[1]);
            DateTime now = DateTime.now();
            if (now.isBefore(date)) {
                return valueBeforeDate;
            } else {
                return valueAfterDate;
            }
        }
    }
}
