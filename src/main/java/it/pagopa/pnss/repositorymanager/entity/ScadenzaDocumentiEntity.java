package it.pagopa.pnss.repositorymanager.entity;

import lombok.*;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;


@DynamoDbBean
@Data
@ToString
@EqualsAndHashCode
public class ScadenzaDocumentiEntity {
    @Getter(AccessLevel.NONE)
    private String documentKey;
    private Long retentionUntil;

    @DynamoDbPartitionKey
    public String getDocumentKey() {
        return documentKey;
    }
}
