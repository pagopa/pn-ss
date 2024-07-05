package it.pagopa.pnss.common.model.pojo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import javax.validation.constraints.NotNull;

/**
 * A pojo class representing the limits options for the indexing service.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE)
public class IndexingLimits {

    @NotNull
    @JsonProperty("MaxTagsPerRequest")
    Long maxTagsPerRequest;
    @NotNull
    @JsonProperty("MaxOperationsOnTagsPerRequest")
    Long maxOperationsOnTagsPerRequest;
    @NotNull
    @JsonProperty("MaxFileKeys")
    Long maxFileKeys;
    @NotNull
    @JsonProperty("MaxMapValuesForSearch")
    Long maxMapValuesForSearch;
    @NotNull
    @JsonProperty("MaxFileKeysUpdateMassivePerRequest")
    Long maxFileKeysUpdateMassivePerRequest;
    @NotNull
    @JsonProperty("MaxTagsPerDocument")
    Long maxTagsPerDocument;
    @NotNull
    @JsonProperty("MaxValuesPerTagDocument")
    Long maxValuesPerTagDocument;
    @NotNull
    @JsonProperty("MaxValuesPerTagPerRequest")
    Long maxValuesPerTagPerRequest;

}
