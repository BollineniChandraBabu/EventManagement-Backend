package com.familywishes.dto;

import com.familywishes.entity.enums.Gender;
import com.familywishes.entity.enums.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
      boolean isBirthdayEnabled) {}

  public record WishSettingsUpdateRequest(
      Boolean isGoodMorningEnabled, Boolean isGoodNightEnabled, Boolean isBirthdayEnabled) {}

  public record UserStatusUpdateRequest(@NotNull Boolean active) {}

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
      String profilePictureUrl) {}
}
