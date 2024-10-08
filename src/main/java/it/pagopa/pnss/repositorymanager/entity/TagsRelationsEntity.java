package it.pagopa.pnss.repositorymanager.entity;

import lombok.*;
import lombok.experimental.FieldDefaults;
import software.amazon.awssdk.enhanced.dynamodb.extensions.annotations.DynamoDbVersionAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

import java.util.List;

@FieldDefaults(level = AccessLevel.PRIVATE)
@DynamoDbBean
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class TagsRelationsEntity {

    @Getter(AccessLevel.NONE)
    String tagKeyValue;
    List<String> fileKeys;
    Long version;

    @DynamoDbPartitionKey
    public String getTagKeyValue() {
        return tagKeyValue;
    }

    @DynamoDbVersionAttribute
    public Long getVersion() {
        return version;
    }

}
