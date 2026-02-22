package com.familywishes.dto;

import com.familywishes.entity.enums.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public class UserDtos {
  public record UserRequest(
      @NotBlank String name,
      @Email String email,
      Role role,
      @NotNull LocalDate dateOfBirth,
      @NotBlank String relationShip,
      boolean isGoodMorningEnabled,
      boolean isGoodNightEnabled,
      boolean isBirthdayEnabled) {}

  public record WishSettingsUpdateRequest(
      Boolean isGoodMorningEnabled, Boolean isGoodNightEnabled, Boolean isBirthdayEnabled) {}

  public record UserResponse(
      Long id,
      String name,
      String email,
      Role role,
      LocalDate dateOfBirth,
      boolean active,
      String relationShip,
      boolean isGoodMorningEnabled,
      boolean isGoodNightEnabled,
      boolean isBirthdayEnabled) {}
}
