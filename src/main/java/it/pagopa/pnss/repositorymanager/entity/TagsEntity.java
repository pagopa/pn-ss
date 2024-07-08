package it.pagopa.pnss.repositorymanager.entity;

import lombok.*;
import software.amazon.awssdk.enhanced.dynamodb.extensions.annotations.DynamoDbVersionAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

import java.util.List;

@DynamoDbBean
@NoArgsConstructor
@AllArgsConstructor
@Data
public class TagsEntity {

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
