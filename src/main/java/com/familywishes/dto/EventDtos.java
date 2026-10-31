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
      @Size(max = 500) String wishesUrl,
      String imageObjectKey) {

    /** Without a wishes link (the link feature is then skipped for this event). */
    public EventRequest(String eventType, LocalDate eventDate, boolean recurring, Long userId) {
      this(eventType, eventDate, recurring, userId, null, null);
    }

    public EventRequest(
        String eventType, LocalDate eventDate, boolean recurring, Long userId, String wishesUrl) {
      this(eventType, eventDate, recurring, userId, wishesUrl, null);
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
      String wishesUrl,
      String imageUrl) {

    public EventResponse(
        Long id,
        String eventType,
        LocalDate eventDate,
        boolean recurring,
        String userName,
        boolean active,
        Long userId,
        String wishesUrl) {
      this(id, eventType, eventDate, recurring, userName, active, userId, wishesUrl, null);
    }
  }
}
