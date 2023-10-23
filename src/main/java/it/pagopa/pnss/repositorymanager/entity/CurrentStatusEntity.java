package it.pagopa.pnss.repositorymanager.entity;

import lombok.Data;
import lombok.ToString;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;

import java.util.List;

@DynamoDbBean
@Data
@ToString
public class CurrentStatusEntity {

    private String storage;
    private List<String> allowedStatusTransitions;
    private String technicalState;
}
