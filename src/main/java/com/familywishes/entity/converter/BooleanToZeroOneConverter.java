package com.familywishes.entity.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class BooleanToZeroOneConverter implements AttributeConverter<Boolean, Short> {

  @Override
  public Short convertToDatabaseColumn(Boolean attribute) {
    return Boolean.TRUE.equals(attribute) ? (short) 1 : (short) 0;
  }

  @Override
  public Boolean convertToEntityAttribute(Short dbData) {
    return dbData != null && dbData == 1;
  }
}
