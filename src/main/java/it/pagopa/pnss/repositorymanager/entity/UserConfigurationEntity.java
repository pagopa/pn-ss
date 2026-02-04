package it.pagopa.pnss.repositorymanager.entity;

import it.pagopa.pnss.common.model.entity.DocumentVersion;
import lombok.*;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

import java.util.List;

@DynamoDbBean
@Data
@ToString
@EqualsAndHashCode(callSuper = true)
public class UserConfigurationEntity extends DocumentVersion {
	
	@Getter(AccessLevel.NONE)
	private String name; // ok
	private List<String> canCreate; // ok
	private List<String> canRead; // ok
	private List<String> canModifyStatus; // ok
	private Boolean canWriteTags;
	private Boolean canReadTags;
	private boolean canExecutePatch; // ok
	/** __DA DEFINIRE__ configurazioni necessarie per la firma digitale */
	private String signatureInfo; // da verificare il tipo
	private UserConfigurationDestinationEntity destination;  // ok
	private String apiKey; // ok
	private Integer durationMinutesUpload;
	private Integer durationMinutesDownload;
	
	@DynamoDbPartitionKey
	public String getName() {
		return name;
	}


}
