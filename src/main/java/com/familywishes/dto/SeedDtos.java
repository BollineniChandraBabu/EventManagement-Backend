package com.familywishes.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class SeedDtos {

  public record SpecialEventSeedRequest(
      @NotBlank String eventName,
      @Min(1) @Max(31) int day,
      @Min(1) @Max(12) int month,
      @NotBlank String message,
      @NotNull Boolean active) {}

  public record SpecialEventSeedResponse(
      Long id, String eventName, int day, int month, String message, boolean active) {}
}
