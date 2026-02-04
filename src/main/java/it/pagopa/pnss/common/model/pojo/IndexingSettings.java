package it.pagopa.pnss.common.model.pojo;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.util.List;

/**
 * A pojo class representing the settings for the indexing service.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE)
@Valid
public class IndexingSettings {

    @NotNull @Valid
    IndexingLimits limits;
    @NotNull
    List<IndexingTag> globals;
    @NotNull
    List<IndexingTag> locals;

}
