package com.familywishes.entity;

import com.familywishes.entity.converter.BooleanToZeroOneConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@MappedSuperclass
@Getter
@Setter
@SuperBuilder
public abstract class ActivatableEntity extends AuditableEntity {

  @Column(nullable = false, columnDefinition = "SMALLINT")
  @Convert(converter = BooleanToZeroOneConverter.class)
  @SuperBuilder.Default
  private boolean active = true;
}
