package com.familywishes.dto;

import jakarta.validation.constraints.NotNull;

public class FestivalDtos {

  public record FestivalResponse(Long id, String eventName, int day, int month, Integer year, boolean active) {}

  public record FestivalWishMappingRequest(
      @NotNull Long specialEventId,
      @NotNull Long instagramUserId,
      String customMessage,
      @NotNull Boolean active) {}

  public record FestivalWishMappingResponse(
      Long id,
      Long specialEventId,
      String festivalName,
      Long instagramUserId,
      String instagramUserName,
      String customMessage,
      boolean active) {}
}
