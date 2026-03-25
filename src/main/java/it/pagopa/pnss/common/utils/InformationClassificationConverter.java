package it.pagopa.pnss.common.utils;

import it.pagopa.pn.safestorage.generated.openapi.server.v1.dto.DocumentType;

public class InformationClassificationConverter
        extends EnumAttributeConverter<DocumentType.InformationClassificationEnum> {
    public InformationClassificationConverter() {
        super(DocumentType.InformationClassificationEnum.class);
    }
}
