package com.familywishes.dto;

import jakarta.validation.constraints.NotBlank;

public class ThemeDtos {

  /** A theme style that the UI can offer. */
  public record ThemeResponse(Long id, String code, String displayName) {}

  /** The theme currently selected by the signed-in user. */
  public record ThemePreferenceResponse(String theme) {}

  /** Request body for saving the signed-in user's selected theme. */
  public record ThemePreferenceRequest(@NotBlank String theme) {}
}
