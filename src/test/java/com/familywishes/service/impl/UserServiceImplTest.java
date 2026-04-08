package com.familywishes.service.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.familywishes.dto.UserDtos.UserRequest;
import com.familywishes.dto.UserDtos.UserResponse;
import com.familywishes.dto.UserDtos.WishSettingsUpdateRequest;
import com.familywishes.entity.RelationshipSeed;
import com.familywishes.entity.User;
import com.familywishes.entity.UserWishSettings;
import com.familywishes.entity.enums.Gender;
import com.familywishes.entity.enums.Role;
import com.familywishes.exception.BadRequestException;
import com.familywishes.repository.RelationshipSeedRepository;
import com.familywishes.repository.UserRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

  @Mock private UserRepository userRepository;
  @Mock private PasswordEncoder passwordEncoder;
  @Mock private RelationshipSeedRepository relationshipSeedRepository;
  @Mock private SupabaseStorageService supabaseStorageService;

  @InjectMocks private UserServiceImpl userService;

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void createShouldPersistBirthdayFlagInWishSettings() {
    UserRequest request =
        new UserRequest(
            "Aman",
            "aman@example.com",
            Role.ROLE_USER,
            Gender.MALE,
            LocalDate.of(1995, 5, 4),
            "BROTHER",
            true,
            false,
            true);

    RelationshipSeed relationship = RelationshipSeed.builder().code("BROTHER").active(true).build();
    when(relationshipSeedRepository.findByCodeAndActiveTrue("BROTHER"))
        .thenReturn(Optional.of(relationship));
    when(passwordEncoder.encode("Test")).thenReturn("encoded-password");
    when(supabaseStorageService.getDefaultProfilePictureUrl(any())).thenReturn("default.png");
    when(userRepository.save(any(User.class)))
        .thenAnswer(
            invocation -> {
              User u = invocation.getArgument(0);
              u.setId(1L);
              return u;
            });

    UserResponse response = userService.create(request);

    assertTrue(response.isBirthdayEnabled());
    assertTrue(response.isGoodMorningEnabled());
    assertFalse(response.isGoodNightEnabled());
    assertEquals(LocalDate.of(1995, 5, 4), response.dateOfBirth());
  }

  @Test
  void updateCurrentUserWishSettingsShouldApplyPartialPatch() {
    RelationshipSeed relationship = RelationshipSeed.builder().code("SON").active(true).build();
    User user =
        User.builder()
            .id(10L)
            .name("Ravi")
            .email("ravi@example.com")
            .role(Role.ROLE_USER)
            .gender(Gender.MALE)
            .birthday(LocalDate.of(1997, 6, 15))
            .relationShip(relationship)
            .active(true)
            .deleted(false)
            .build();
    user.setWishSettings(
        UserWishSettings.builder()
            .user(user)
            .goodMorningEnabled(false)
            .goodNightEnabled(true)
            .birthdayEnabled(false)
            .build());

    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(
                "ravi@example.com", "password", List.of(new SimpleGrantedAuthority("ROLE_USER"))));

    when(userRepository.findByEmailAndDeletedFalse("ravi@example.com"))
        .thenReturn(Optional.of(user));
    when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
    when(supabaseStorageService.getDefaultProfilePictureUrl(any())).thenReturn("default.png");

    UserResponse response =
        userService.updateCurrentUserWishSettings(new WishSettingsUpdateRequest(true, null, true));

    assertTrue(response.isGoodMorningEnabled());
    assertTrue(response.isGoodNightEnabled());
    assertTrue(response.isBirthdayEnabled());
  }

  @Test
  void updateShouldRejectRoleChangeForNonAdmin() {
    RelationshipSeed relationship = RelationshipSeed.builder().code("BROTHER").active(true).build();
    User existingUser =
        User.builder()
            .id(22L)
            .name("Aman")
            .email("aman@example.com")
            .role(Role.ROLE_USER)
            .gender(Gender.MALE)
            .birthday(LocalDate.of(1995, 5, 4))
            .relationShip(relationship)
            .active(true)
            .deleted(false)
            .build();

    UserRequest request =
        new UserRequest(
            "Aman Updated",
            "aman@example.com",
            Role.ROLE_ADMIN,
            Gender.MALE,
            LocalDate.of(1995, 5, 4),
            "BROTHER",
            true,
            true,
            true);

    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(
                "aman@example.com", "password", List.of(new SimpleGrantedAuthority("ROLE_USER"))));

    when(userRepository.findByEmail("aman@example.com")).thenReturn(Optional.of(existingUser));

    assertThrows(BadRequestException.class, () -> userService.update(request));
  }
}
