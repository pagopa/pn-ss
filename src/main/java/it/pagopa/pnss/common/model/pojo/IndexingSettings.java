package it.pagopa.pnss.common.model.pojo;

import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotNull;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE)
@Validated
public class IndexingSettings {

    @NotNull
    IndexingLimits limits;
    @NotNull
    List<IndexingTag> globals;
    @NotNull
    List<IndexingTag> locals;

}
