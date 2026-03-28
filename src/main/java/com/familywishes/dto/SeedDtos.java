package com.familywishes.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public class SeedDtos {

  public record EnumSeedRequest(
      @NotBlank String code, @NotBlank String displayName, @NotNull Boolean active) {}

  public record EnumSeedResponse(
      Long id, String category, String code, String displayName, boolean active) {}

  public record SpecialEventSeedRequest(
      @NotBlank String eventName,
      @NotNull LocalDate eventDate,
      @NotBlank String message,
      @NotNull Boolean active) {}

  public record SpecialEventSeedResponse(
      Long id, String eventName, LocalDate eventDate, String message, boolean active) {}
}
