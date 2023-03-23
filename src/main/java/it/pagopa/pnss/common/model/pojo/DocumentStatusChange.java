package it.pagopa.pnss.common.model.pojo;

import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
@Data
@ToString(callSuper = true)
@SuperBuilder
public class DocumentStatusChange {

    String xPagopaExtchCxId;
    String processId;
    String currentStatus;
    String nextStatus;
}
