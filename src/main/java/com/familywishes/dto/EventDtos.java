package com.familywishes.dto;

import com.familywishes.entity.enums.EventType;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public class EventDtos {
  public record EventRequest(
      @NotNull EventType eventType,
      String festivalName,
      LocalDate eventDate,
      boolean recurring,
      @NotNull Long userId) {}

  public record EventResponse(
      Long id,
      EventType eventType,
      String festivalName,
      LocalDate eventDate,
      boolean recurring,
      Long userId,
      boolean active) {}
}
