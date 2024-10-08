package it.pagopa.pnss.common.model.pojo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

/**
 * A pojo class representing a tag for the indexing service
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE)
public class IndexingTag {

    String key;
    boolean indexed;
    boolean multivalue;
    boolean global;

}
