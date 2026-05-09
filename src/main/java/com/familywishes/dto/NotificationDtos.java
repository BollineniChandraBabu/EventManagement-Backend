package com.familywishes.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

public class NotificationDtos {
  public record NotificationRequest(
      @NotBlank @Size(max = 150) String title, @NotBlank String message, Boolean canSendEmail) {}

  public record NotificationResponse(
      Long id,
      String title,
      String message,
      Boolean published,
      Boolean canSendEmail,
      LocalDateTime publishedAt,
      String createdBy,
      String updatedBy,
      LocalDateTime createdAt,
      LocalDateTime updatedAt) {}
}
