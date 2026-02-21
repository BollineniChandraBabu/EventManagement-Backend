package com.familywishes.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class SeedDtos {

  public record EnumSeedRequest(@NotBlank String code, @NotBlank String displayName, @NotNull Boolean active) {}

  public record EnumSeedResponse(Long id, String category, String code, String displayName, boolean active) {}

  public record SpecialEventSeedRequest(
      @NotBlank String eventName,
      @Min(1) @Max(31) int day,
      @Min(1) @Max(12) int month,
      @NotBlank String message,
      @NotNull Boolean active) {}

  public record SpecialEventSeedResponse(
      Long id, String eventName, int day, int month, String message, boolean active) {}

  public record WishTemplateSeedRequest(
      @NotBlank String type,
      @NotBlank String relation,
      @NotBlank String event,
      @NotBlank String tone,
      @NotBlank String language,
      @NotNull Boolean active) {}

  public record WishTemplateSeedResponse(
      Long id,
      String type,
      String relation,
      String event,
      String tone,
      String language,
      boolean active) {}
}
