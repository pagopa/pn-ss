package it.pagopa.pn.library.sign.configurationproperties;

import lombok.CustomLog;
import lombok.Data;
import org.joda.time.DateTime;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.Pattern;
import java.util.*;

@ConfigurationProperties(prefix = "pn.sign")
@Validated
@CustomLog
@Data
public class PnSignServiceConfigurationProperties {

    @Pattern(regexp = "(\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z;[a-zA-Z]+)(?:,(?:\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z;[a-zA-Z]+))*")
    private String providerSwitch;

    private String actualProvider;

    public String getProviderSwitch() {
        return returnPropertyValue(providerSwitch);
    }

    private TreeMap<DateTime, String> splitDateProviders(String propertyString) {
        TreeMap<DateTime, String> dateProviders = new TreeMap<DateTime, String>();
        String[] propertyArray = propertyString.split(",");
        for (String property : propertyArray) {
            DateTime key;
            String value = "";
            String[] propertyBase = property.split(";");
            key = DateTime.parse(propertyBase[0]);
            value = (propertyBase[1].toLowerCase());

            dateProviders.put(key, value);
        }
        return dateProviders;
    }

    private String returnPropertyValue(String propertyString) {
        String provider = "";
        SortedMap<DateTime, String> dateProviderMap = splitDateProviders(propertyString).descendingMap();
        DateTime now = DateTime.now();

        for (Map.Entry<DateTime, String> entry : dateProviderMap.entrySet()) {
            if (entry.getKey().isBefore(now)) {
                provider = entry.getValue();
                break;
            }
        }
        return provider;
    }

}
