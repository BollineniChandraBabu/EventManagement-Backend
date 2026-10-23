package com.familywishes.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;

public class FestivalDtos {

  /** Serializable because lists of it are kept in the festivalsByMonth cache. */
  public record FestivalResponse(Long id, String eventName, LocalDate eventDate, boolean active)
      implements Serializable {}

  /** customMessage is an optional personal note added to the festival email. */
  public record FestivalWishMappingRequest(
      @NotNull Long specialEventId,
      @NotNull Long userId,
      @NotNull Boolean active,
      @Size(max = 500) String customMessage) {

    public FestivalWishMappingRequest(Long specialEventId, Long userId, Boolean active) {
      this(specialEventId, userId, active, null);
    }
  }

  /** Maps one festival to many users at once (existing mappings are updated). */
  public record FestivalWishMappingBulkRequest(
      @NotNull Long specialEventId,
      @NotEmpty @Size(max = 500) List<Long> userIds,
      @NotNull Boolean active,
      @Size(max = 500) String customMessage) {}

  public record FestivalWishMappingBulkResponse(int created, int updated) {}

  /** Numbers for the dashboard header. upcoming = active mappings whose festival is in the next 30 days. */
  public record FestivalWishMappingSummary(
      long total, long active, long inactive, long upcoming, long failed) {}

  public record FestivalWishMappingResponse(
      Long id,
      Long specialEventId,
      String festivalName,
      LocalDate eventDate,
      Long userId,
      String userName,
      boolean active,
      String customMessage,
      LocalDate lastWishSentOn,
      boolean error) {}
}
