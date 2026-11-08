package com.familywishes.dto;

import com.familywishes.entity.enums.Gender;
import com.familywishes.entity.enums.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class UserDtos {
  public record UserRequest(
      @NotBlank String name,
      @Email String email,
      Role role,
      @NotNull Gender gender,
      @NotNull LocalDate dateOfBirth,
      @NotBlank String relationShip,
      boolean isGoodMorningEnabled,
      boolean isGoodNightEnabled,
      boolean isBirthdayEnabled,
      @Size(max = 500) String birthdayWishesUrl,
      boolean mfaEnabled,
      boolean concurrentLoginAllowed) {

    /** Without a wishes link or MFA/concurrent-login choice (both default to off/on). */
    public UserRequest(
        String name,
        String email,
        Role role,
        Gender gender,
        LocalDate dateOfBirth,
        String relationShip,
        boolean isGoodMorningEnabled,
        boolean isGoodNightEnabled,
        boolean isBirthdayEnabled) {
      this(
          name,
          email,
          role,
          gender,
          dateOfBirth,
          relationShip,
          isGoodMorningEnabled,
          isGoodNightEnabled,
          isBirthdayEnabled,
          null,
          false,
          true);
    }

    /** Without an MFA or concurrent-login choice (MFA off, concurrent logins allowed). */
    public UserRequest(
        String name,
        String email,
        Role role,
        Gender gender,
        LocalDate dateOfBirth,
        String relationShip,
        boolean isGoodMorningEnabled,
        boolean isGoodNightEnabled,
        boolean isBirthdayEnabled,
        String birthdayWishesUrl) {
      this(
          name,
          email,
          role,
          gender,
          dateOfBirth,
          relationShip,
          isGoodMorningEnabled,
          isGoodNightEnabled,
          isBirthdayEnabled,
          birthdayWishesUrl,
          false,
          true);
    }
  }

  public record WishSettingsUpdateRequest(
      Boolean isGoodMorningEnabled, Boolean isGoodNightEnabled, Boolean isBirthdayEnabled) {}

  public record MfaUpdateRequest(@NotNull Boolean mfaEnabled) {}

  public record ConcurrentLoginUpdateRequest(@NotNull Boolean concurrentLoginAllowed) {}

  public record UserStatusUpdateRequest(@NotNull Boolean active) {}

  public record WishPreviewResponse(
      boolean showMessage, String wishType, String subject, String htmlMessage, byte[] imageData) {}

  public record ProfilePictureUploadUrlRequest(String fileName, String contentType) {}

  public record ProfilePictureUploadUrlResponse(
      String uploadUrl, String publicUrl, String objectKey, String method, long expiresInSeconds) {}

  public record UserResponse(
      Long id,
      String name,
      String email,
      Role role,
      Gender gender,
      LocalDate dateOfBirth,
      boolean active,
      String relationShip,
      boolean isGoodMorningEnabled,
      boolean isGoodNightEnabled,
      boolean isBirthdayEnabled,
      boolean online,
      LocalDateTime lastSeenAt,
      String profilePictureUrl,
      String birthdayWishesUrl,
      boolean mfaEnabled,
      boolean concurrentLoginAllowed) {}
}
