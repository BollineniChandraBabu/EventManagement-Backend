package com.familywishes.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public class CalendarDtos {

  /**
   * @param recurring false = happens only on eventDate (festivals, one-time events); true = repeats
   *     every year (birthdays, recurring events)
   * @param ownerName the person the entry belongs to (events and birthdays), null for festivals
   */
  public record CalendarResponse(
      Long id,
      String eventType,
      String eventName,
      LocalDate eventDate,
      boolean active,
      boolean recurring,
      String ownerName) {}
}
