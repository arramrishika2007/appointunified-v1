package com.appointunified.config;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * JPA AttributeConverter for generic Enum handling with PostgreSQL.
 * This is a marker class - individual converters extend this.
 */
public abstract class PostgresEnumType<T extends Enum<T>> implements AttributeConverter<T, String> {

    private final Class<T> enumClass;

    protected PostgresEnumType(Class<T> enumClass) {
        this.enumClass = enumClass;
    }

    @Override
    public String convertToDatabaseColumn(T enumValue) {
        if (enumValue == null) {
            return null;
        }
        return enumValue.name();
    }

    @Override
    public T convertToEntityAttribute(String dbValue) {
        if (dbValue == null) {
            return null;
        }
        return Enum.valueOf(enumClass, dbValue);
    }
}
