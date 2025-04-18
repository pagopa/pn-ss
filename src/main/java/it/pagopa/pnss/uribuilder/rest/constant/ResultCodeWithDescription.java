package it.pagopa.pnss.uribuilder.rest.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Singular;

@AllArgsConstructor
@Getter
public enum ResultCodeWithDescription {

    OK("200.1", "OK");

    final String resultCode;
    final String description;

}
