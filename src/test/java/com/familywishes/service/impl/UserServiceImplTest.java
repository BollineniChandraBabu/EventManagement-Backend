package com.familywishes.service.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.familywishes.dto.UserDtos.UserRequest;
import com.familywishes.dto.UserDtos.UserResponse;
import com.familywishes.dto.UserDtos.WishSettingsUpdateRequest;
import com.familywishes.entity.User;
import com.familywishes.entity.UserWishSettings;
import com.familywishes.entity.enums.RelationShip;
import com.familywishes.entity.enums.Role;
import com.familywishes.repository.UserRepository;
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

  @InjectMocks private UserServiceImpl userService;

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void createShouldPersistBirthdayFlagInWishSettings() {
    UserRequest request =
        new UserRequest(
            "Aman", "aman@example.com", Role.ROLE_USER, RelationShip.BROTHER, true, false, true);

    when(passwordEncoder.encode("Test")).thenReturn("encoded-password");
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
  }

  @Test
  void updateCurrentUserWishSettingsShouldApplyPartialPatch() {
    User user =
        User.builder()
            .id(10L)
            .name("Ravi")
            .email("ravi@example.com")
            .role(Role.ROLE_USER)
            .relationShip(RelationShip.SON)
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

    UserResponse response =
        userService.updateCurrentUserWishSettings(new WishSettingsUpdateRequest(true, null, true));

    assertTrue(response.isGoodMorningEnabled());
    assertTrue(response.isGoodNightEnabled());
    assertTrue(response.isBirthdayEnabled());
  }
}
