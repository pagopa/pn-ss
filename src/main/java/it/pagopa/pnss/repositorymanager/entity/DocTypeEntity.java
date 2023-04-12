package it.pagopa.pnss.repositorymanager.entity;


import it.pagopa.pn.template.internal.rest.v1.dto.DocumentType.InformationClassificationEnum;
import it.pagopa.pn.template.internal.rest.v1.dto.DocumentType.TimeStampedEnum;
import it.pagopa.pn.template.internal.rest.v1.dto.DocumentType.TransformationsEnum;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.FieldDefaults;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

import java.util.List;
import java.util.Map;

@FieldDefaults(level = AccessLevel.PRIVATE)
@DynamoDbBean
@Data
@ToString
public class DocTypeEntity {

    @Getter(onMethod = @__({@DynamoDbPartitionKey}))
    String tipoDocumento;
    String checksum;
    String initialStatus;
    Map<String, CurrentStatusEntity> statuses;
    InformationClassificationEnum informationClassification;
    List<TransformationsEnum> transformations;
    TimeStampedEnum timeStamped;
}
