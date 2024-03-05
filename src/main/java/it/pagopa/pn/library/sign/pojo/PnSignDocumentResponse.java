package it.pagopa.pn.library.sign.pojo;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
@AllArgsConstructor
public class PnSignDocumentResponse {

    @ToString.Exclude
    byte[] signedDocument;

}
