package it.pagopa.pnss.transformation.model.dto;

import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE)
@Data
@NoArgsConstructor
public class TransformationMessage {

    String fileKey;
    String transformationType;
    String bucketName;
    String contentType;
}
