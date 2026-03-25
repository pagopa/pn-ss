package it.pagopa.pnss.common.utils;

import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.DocumentType;

public class TimeStampedConverter
        extends EnumAttributeConverter<DocumentType.TimeStampedEnum> {
    public TimeStampedConverter() {
        super(DocumentType.TimeStampedEnum.class);
    }
}
