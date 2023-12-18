package it.pagopa.pnss.common.exception;

public class InvalidConfigurationException extends RuntimeException {

    public InvalidConfigurationException(String configValue) {
        super(String.format("The configuration value '%s' is not valid", configValue));
    }

}
