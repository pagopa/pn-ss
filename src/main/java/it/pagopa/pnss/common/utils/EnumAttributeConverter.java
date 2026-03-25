package it.pagopa.pnss.common.utils;

import software.amazon.awssdk.enhanced.dynamodb.AttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.AttributeValueType;
import software.amazon.awssdk.enhanced.dynamodb.EnhancedType;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

public class EnumAttributeConverter<T extends Enum<T>> implements AttributeConverter<T> {

    private final Class<T> enumClass;

    public EnumAttributeConverter(Class<T> enumClass) {
        this.enumClass = enumClass;
    }

    @Override
    public AttributeValue transformFrom(T input) {
        return AttributeValue.builder().s(input.name()).build();
    }

    @Override
    public T transformTo(AttributeValue input) {
        return Enum.valueOf(enumClass, input.s());
    }

    @Override
    public EnhancedType<T> type() {
        return EnhancedType.of(enumClass);
    }

    @Override
    public AttributeValueType attributeValueType() {
        return AttributeValueType.S;
    }
}
