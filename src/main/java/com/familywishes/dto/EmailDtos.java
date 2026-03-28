package com.familywishes.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;

public class EmailDtos {
  public record SendEmailNowRequest(
      @Email @NotBlank String userEmail,
      @NotBlank String subject,
      @NotBlank String body,
      @NotBlank String eventTypeCode) {}

  public record EmailStatusResponse(
      long id,
      String toEmail,
      String subject,
      String body,
      String imageUrl,
      String status,
      String emailType,
      LocalDateTime sentAt) {}
}
