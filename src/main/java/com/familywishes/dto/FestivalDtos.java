package com.familywishes.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public class FestivalDtos {

  public record FestivalResponse(Long id, String eventName, LocalDate eventDate, boolean active) {}

  public record FestivalWishMappingRequest(
      @NotNull Long specialEventId,
      @NotNull Long userId,
      String customMessage,
      @NotNull Boolean active) {}

  public record FestivalWishMappingResponse(
      Long id,
      Long specialEventId,
      String festivalName,
      Long userId,
      String userName,
      String customMessage,
      boolean active) {}
}
