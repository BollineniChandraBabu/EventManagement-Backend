package com.familywishes.dto;

import jakarta.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.LocalDate;

public class FestivalDtos {

  /** Serializable because lists of it are kept in the festivalsByMonth cache. */
  public record FestivalResponse(Long id, String eventName, LocalDate eventDate, boolean active)
      implements Serializable {}

  public record FestivalWishMappingRequest(
      @NotNull Long specialEventId, @NotNull Long userId, @NotNull Boolean active) {}

  public record FestivalWishMappingResponse(
      Long id,
      Long specialEventId,
      String festivalName,
      LocalDate eventDate,
      Long userId,
      String userName,
      boolean active) {}
}
