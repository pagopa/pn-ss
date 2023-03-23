package it.pagopa.pnss.common.configurationproperties.endpoint.internal.statemachine;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "internal-endpoint.state-machine")
public record StateMachineEndpointProperties(String containerBaseUrl, String validate) {
}
