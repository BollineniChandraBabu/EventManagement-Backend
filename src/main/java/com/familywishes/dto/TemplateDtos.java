package com.familywishes.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.Map;

public class TemplateDtos {
    public record TemplateRequest(@NotBlank String name, @NotBlank String subject, @NotBlank String htmlContent) {}
    public record TemplateResponse(Long id, String name, String subject, String htmlContent, Integer version) {}
    public record PreviewRequest(@NotBlank String htmlContent, Map<String, String> variables) {}
}
