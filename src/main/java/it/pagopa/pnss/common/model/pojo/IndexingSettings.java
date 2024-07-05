package it.pagopa.pnss.common.model.pojo;

import lombok.*;
import lombok.experimental.FieldDefaults;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
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
