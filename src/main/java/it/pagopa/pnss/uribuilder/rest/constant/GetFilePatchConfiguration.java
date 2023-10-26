package it.pagopa.pnss.uribuilder.rest.constant;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum GetFilePatchConfiguration {

    ALL("ALL"),
    CLIENT_SPECIFIC("CLIENT_SPECIFIC"),
    NONE("NONE");

    private final String value;

}
