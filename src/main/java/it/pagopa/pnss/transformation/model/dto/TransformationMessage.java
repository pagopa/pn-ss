package it.pagopa.pnss.transformation.model.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class TransformationMessage {

    String fileKey;
    String transformationType;
    String bucketName;
    String contentType;
}
