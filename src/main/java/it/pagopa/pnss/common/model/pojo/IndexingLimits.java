package it.pagopa.pnss.common.model.pojo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import javax.validation.constraints.NotNull;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE)
public class IndexingLimits {

    @NotNull
    Long maxTagsPerRequest;
    @NotNull
    Long maxOperationsOnTagsPerRequest;
    @NotNull
    Long maxFileKeys;
    @NotNull
    Long maxMapValuesForSearch;
    @NotNull
    Long maxFileKeysUpdateMassivePerRequest;
    @NotNull
    Long maxTagsPerDocument;
    @NotNull
    Long maxValuesPerTagDocument;
    @NotNull
    Long maxValuesPerTagPerRequest;

}
