package it.pagopa.pnss.repositorymanager;

import it.pagopa.pnss.repositorymanager.entity.DocumentEntity;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class NullSafeOffsetDateTimeConverterTest {

    private static final String VALID_VALUE = "2025-11-06T05:08:58.064313133Z";

    private DocumentEntity mapItem(Map<String, AttributeValue> item) {
        TableSchema<DocumentEntity> schema = TableSchema.fromBean(DocumentEntity.class);
        Map<String, AttributeValue> base = new HashMap<>();
        base.put("documentKey", AttributeValue.fromS("k"));
        base.put("documentState", AttributeValue.fromS("deleted"));
        base.putAll(item);
        return schema.mapToItem(base);
    }

    @Test
    void emptyStringTimestamp_isReadAsNull() {
        DocumentEntity entity = mapItem(Map.of("lastStatusChangeTimestamp", AttributeValue.fromS("")));

        assertThat(entity.getLastStatusChangeTimestamp()).isNull();
    }

    @Test
    void nullTypeTimestamp_isReadAsNull() {
        DocumentEntity entity = mapItem(Map.of("lastStatusChangeTimestamp", AttributeValue.fromNul(true)));

        assertThat(entity.getLastStatusChangeTimestamp()).isNull();
    }

    @Test
    void missingTimestamp_isReadAsNull() {
        DocumentEntity entity = mapItem(Map.of());

        assertThat(entity.getLastStatusChangeTimestamp()).isNull();
    }

    @Test
    void validTimestamp_isPreserved() {
        DocumentEntity entity = mapItem(Map.of("lastStatusChangeTimestamp", AttributeValue.fromS(VALID_VALUE)));

        assertThat(entity.getLastStatusChangeTimestamp()).isEqualTo(OffsetDateTime.parse(VALID_VALUE));
    }

    @Test
    void roundTripWriteThenRead_preservesValidTimestamp() {
        TableSchema<DocumentEntity> schema = TableSchema.fromBean(DocumentEntity.class);
        DocumentEntity source = new DocumentEntity();
        source.setDocumentKey("k");
        source.setDocumentState("deleted");
        source.setLastStatusChangeTimestamp(OffsetDateTime.parse(VALID_VALUE));

        Map<String, AttributeValue> written = schema.itemToMap(source, true);
        DocumentEntity readBack = schema.mapToItem(written);

        assertThat(readBack.getLastStatusChangeTimestamp()).isEqualTo(OffsetDateTime.parse(VALID_VALUE));
    }
}
