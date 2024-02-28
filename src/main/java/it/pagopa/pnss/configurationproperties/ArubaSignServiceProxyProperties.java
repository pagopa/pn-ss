package it.pagopa.pnss.configurationproperties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

@ConfigurationProperties("aruba.proxy")
public class ArubaSignServiceProxyProperties {
    private final HashMap<String, Object> properties = new HashMap<>();
    public Map<String, Object> getProperties() {
        return this.properties;
    }
}
