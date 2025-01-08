package it.pagopa.pn.library.sign.pojo;

import lombok.Getter;

/**
 * Enum to define the type of signature
 */
@Getter
public enum SignatureType {

    CADES("CADES"),
    PADES("PADES"),
    XADES("XADES");

    private final String value;

    SignatureType(String value) {
        this.value = value;
    }

}
