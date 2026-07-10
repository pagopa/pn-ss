package it.pagopa.pnss.common.utils;

import software.amazon.awssdk.enhanced.dynamodb.AttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.AttributeValueType;
import software.amazon.awssdk.enhanced.dynamodb.EnhancedType;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.time.OffsetDateTime;

public class NullSafeOffsetDateTimeConverter implements AttributeConverter<OffsetDateTime> {

    @Override
    public AttributeValue transformFrom(OffsetDateTime input) {
        return AttributeValue.builder().s(input.toString()).build();
    }

    @Override
    public OffsetDateTime transformTo(AttributeValue input) {
        if (input == null || Boolean.TRUE.equals(input.nul()) || input.s() == null || input.s().isBlank()) {
            return null;
        }
        return OffsetDateTime.parse(input.s());
    }

    @Override
    public EnhancedType<OffsetDateTime> type() {
        return EnhancedType.of(OffsetDateTime.class);
    }

    @Override
    public AttributeValueType attributeValueType() {
        return AttributeValueType.S;
    }
}
