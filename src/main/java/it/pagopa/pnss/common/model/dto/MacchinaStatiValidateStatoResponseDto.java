package it.pagopa.pnss.common.model.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
@EqualsAndHashCode
public class MacchinaStatiValidateStatoResponseDto {

    String message;
    boolean allowed;
    String notificationMessage;
}
