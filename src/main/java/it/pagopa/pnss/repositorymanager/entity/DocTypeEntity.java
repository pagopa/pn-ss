package it.pagopa.pnss.repositorymanager.entity;


import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTypeConvertedEnum;
import it.pagopa.pn.template.internal.rest.v1.dto.DocumentType;
import it.pagopa.pn.template.internal.rest.v1.dto.DocumentType.InformationClassificationEnum;
import it.pagopa.pn.template.internal.rest.v1.dto.DocumentType.TimeStampedEnum;
import lombok.Data;
import lombok.Getter;
import lombok.ToString;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

import java.util.Map;

@DynamoDbBean
@Data
@ToString
public class DocTypeEntity {

	@Getter(onMethod=@__({@DynamoDbPartitionKey}))
	private String tipoDocumento;
	private String checksum;
	private String initialStatus;
	private Map<String, CurrentStatusEntity> statuses;
	@Getter(onMethod=@__({@DynamoDBTypeConvertedEnum}))
	private InformationClassificationEnum informationClassification;
//	private Boolean digitalSignature;
	@Getter(onMethod=@__({@DynamoDBTypeConvertedEnum}))
	private DocumentType.TransformationsEnum transformations;
	@Getter(onMethod=@__({@DynamoDBTypeConvertedEnum}))
	private TimeStampedEnum timeStamped;
}
