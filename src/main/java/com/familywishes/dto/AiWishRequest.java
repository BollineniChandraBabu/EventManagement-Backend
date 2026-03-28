package com.familywishes.dto;

import jakarta.validation.constraints.NotBlank;

public record AiWishRequest(
    @NotBlank String name,
    @NotBlank String relation,
    String event,
    String festival,
    @NotBlank String tone,
    @NotBlank String language) {}
