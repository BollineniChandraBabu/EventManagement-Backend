package com.familywishes.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public class CalendarDtos {

  public record CalendarResponse(Long id, String eventType, String eventName, LocalDate eventDate, boolean active) {}
}
