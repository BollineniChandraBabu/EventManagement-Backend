package com.familywishes.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public class EventDtos {
  public record EventRequest(
      @NotBlank String eventType,
      String festivalName,
      LocalDate eventDate,
      boolean recurring,
      @NotNull Long userId) {}

  public record EventResponse(
      Long id,
      String eventType,
      String festivalName,
      LocalDate eventDate,
      boolean recurring,
      Long userId,
      boolean active) {}
}
