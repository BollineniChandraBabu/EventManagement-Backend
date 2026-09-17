package com.familywishes.dto;

import jakarta.validation.constraints.NotBlank;

public record AiWishRequest(
    @NotBlank String name,
    @NotBlank String relation,
    String event,
    String festival,
    @NotBlank String tone,
    @NotBlank String language,
    Long userId) {
  public AiWishRequest(
      String name, String relation, String event, String festival, String tone, String language) {
    this(name, relation, event, festival, tone, language, null);
  }
}
