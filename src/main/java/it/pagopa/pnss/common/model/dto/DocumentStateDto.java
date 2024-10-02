package it.pagopa.pnss.common.model.dto;

import it.pagopa.pnss.repositorymanager.entity.DocumentEntity;
import lombok.*;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
@EqualsAndHashCode
public class DocumentStateDto {

    DocumentEntity documentEntity;
    String oldDocumentState;
}
