package com.familywishes.dto;

import com.familywishes.entity.enums.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class UserDtos {
  public record UserRequest(
      @NotBlank String name,
      @Email String email,
      Role role,
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
      boolean active,
      String relationShip,
      boolean isGoodMorningEnabled,
      boolean isGoodNightEnabled,
      boolean isBirthdayEnabled) {}
}
