package com.familywishes.dto;

import com.familywishes.entity.enums.RelationShip;
import com.familywishes.entity.enums.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class UserDtos {
    public record UserRequest(@NotBlank String name, @Email String email, Role role, RelationShip relationShip, boolean isGoodMorningEnabled, boolean isGoodNightEnabled) {}
    public record UserResponse(Long id, String name, String email, Role role, boolean active, String relationShip, boolean isGoodMorningEnabled, boolean isGoodNightEnabled) {}
}
