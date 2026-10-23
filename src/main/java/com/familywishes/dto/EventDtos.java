package com.familywishes.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public class EventDtos {
  public record EventRequest(
      @NotBlank String eventType,
      LocalDate eventDate,
      boolean recurring,
      @NotNull Long userId,
      @Size(max = 500) String wishesUrl) {

    /** Without a wishes link (the link feature is then skipped for this event). */
    public EventRequest(String eventType, LocalDate eventDate, boolean recurring, Long userId) {
      this(eventType, eventDate, recurring, userId, null);
    }
  }

  public record EventResponse(
      Long id,
      String eventType,
      LocalDate eventDate,
      boolean recurring,
      String userName,
      boolean active,
      Long userId,
      String wishesUrl) {}
}
