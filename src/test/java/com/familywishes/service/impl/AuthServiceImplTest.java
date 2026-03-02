package com.familywishes.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.familywishes.dto.AuthDtos.AdminLoginAsUserRequest;
import com.familywishes.dto.AuthDtos.AuthResponse;
import com.familywishes.dto.AuthDtos.LoginRequest;
import com.familywishes.entity.RefreshToken;
import com.familywishes.entity.User;
import com.familywishes.entity.enums.Role;
import com.familywishes.exception.BadRequestException;
import com.familywishes.repository.OtpCodeRepository;
import com.familywishes.repository.PasswordResetTokenRepository;
import com.familywishes.repository.RefreshTokenRepository;
import com.familywishes.repository.UserRepository;
import com.familywishes.security.JwtService;
import com.familywishes.service.GmailEmailService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

  @Mock private AuthenticationManager authManager;
  @Mock private UserRepository userRepository;
  @Mock private JwtService jwtService;
  @Mock private RefreshTokenRepository refreshTokenRepository;
  @Mock private OtpCodeRepository otpCodeRepository;
  @Mock private PasswordResetTokenRepository resetTokenRepository;
  @Mock private PasswordEncoder passwordEncoder;
  @Mock private GmailEmailService emailService;

  @InjectMocks private AuthServiceImpl authService;

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void loginShouldIncrementFailedAttemptsWhenPasswordIsWrong() {
    User user =
        User.builder()
            .id(4L)
            .email("user@example.com")
            .password("encoded")
            .role(Role.ROLE_USER)
            .failedLoginAttempts(1)
            .build();

    when(userRepository.findByEmailAndDeletedFalse("user@example.com")).thenReturn(Optional.of(user));
    when(authManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
        .thenThrow(new BadCredentialsException("bad credentials"));

    assertThrows(
        BadRequestException.class,
        () ->
            authService.login(
                new LoginRequest("user@example.com", "wrong")));

    assertEquals(2, user.getFailedLoginAttempts());
    verify(userRepository).save(user);
    verify(emailService, never())
        .sendEmailWithAttachments(any(String.class), any(String.class), any(String.class), any(), any());
  }

  @Test
  void loginShouldSendEmailAndBlockWhenThresholdIsReached() {
    User user =
        User.builder()
            .id(5L)
            .email("user@example.com")
            .password("encoded")
            .role(Role.ROLE_USER)
            .failedLoginAttempts(4)
            .build();

    when(userRepository.findByEmailAndDeletedFalse("user@example.com")).thenReturn(Optional.of(user));
    when(authManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
        .thenThrow(new BadCredentialsException("bad credentials"));

    BadRequestException exception =
        assertThrows(
            BadRequestException.class,
            () ->
                authService.login(
                    new LoginRequest("user@example.com", "wrong")));

    assertEquals("Too many failed login attempts. Please contact the Administrator.", exception.getMessage());
    assertEquals(5, user.getFailedLoginAttempts());
    verify(emailService)
        .sendEmailWithAttachments(
            any(String.class), any(String.class), any(String.class), any(), any());
  }

  @Test
  void loginShouldBlockImmediatelyWhenUserAlreadyReachedThreshold() {
    User user =
        User.builder()
            .id(6L)
            .email("user@example.com")
            .password("encoded")
            .role(Role.ROLE_USER)
            .failedLoginAttempts(5)
            .build();

    when(userRepository.findByEmailAndDeletedFalse("user@example.com")).thenReturn(Optional.of(user));

    BadRequestException exception =
        assertThrows(
            BadRequestException.class,
            () ->
                authService.login(
                    new LoginRequest("user@example.com", "password")));

    assertEquals("Too many failed login attempts. Please contact the Administrator.", exception.getMessage());
    verify(authManager, never()).authenticate(any(UsernamePasswordAuthenticationToken.class));
  }

  @Test
  void adminLoginAsUserShouldGenerateTokensForUserAccount() {
    User admin = User.builder().id(1L).email("admin@example.com").role(Role.ROLE_ADMIN).build();
    User normalUser =
        User.builder().id(2L).email("user@example.com").role(Role.ROLE_USER).active(true).build();

    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(
                "admin@example.com",
                "password",
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));

    when(userRepository.findByEmailAndDeletedFalse("admin@example.com"))
        .thenReturn(Optional.of(admin));
    when(userRepository.findByEmailAndDeletedFalse("user@example.com"))
        .thenReturn(Optional.of(normalUser));
    when(jwtService.generateAccessToken(normalUser, "admin@example.com"))
        .thenReturn("access-token");
    when(jwtService.generateRefreshToken(normalUser, "admin@example.com"))
        .thenReturn("refresh-token");
    when(jwtService.getAccessTokenTtlSeconds()).thenReturn(900L);
    when(refreshTokenRepository.save(any(RefreshToken.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    AuthResponse response =
        authService.adminLoginAsUser(new AdminLoginAsUserRequest("user@example.com"));

    assertEquals("access-token", response.accessToken());
    assertEquals("refresh-token", response.refreshToken());
    assertEquals("ROLE_USER", response.role());
  }

  @Test
  void adminLoginAsUserShouldRejectTargetAdminAccount() {
    User admin = User.builder().id(1L).email("admin@example.com").role(Role.ROLE_ADMIN).build();
    User targetAdmin =
        User.builder().id(3L).email("other-admin@example.com").role(Role.ROLE_ADMIN).build();

    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(
                "admin@example.com",
                "password",
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));

    when(userRepository.findByEmailAndDeletedFalse("admin@example.com"))
        .thenReturn(Optional.of(admin));
    when(userRepository.findByEmailAndDeletedFalse("other-admin@example.com"))
        .thenReturn(Optional.of(targetAdmin));

    assertThrows(
        BadRequestException.class,
        () -> authService.adminLoginAsUser(new AdminLoginAsUserRequest("other-admin@example.com")));
  }

  @Test
  void switchBackToAdminShouldReturnAdminTokensForImpersonatedSession() {
    User admin =
        User.builder().id(1L).email("admin@example.com").role(Role.ROLE_ADMIN).active(true).build();

    when(jwtService.isValid("impersonated-access-token")).thenReturn(true);
    when(jwtService.extractImpersonatedBy("impersonated-access-token"))
        .thenReturn("admin@example.com");
    when(userRepository.findByEmailAndDeletedFalse("admin@example.com"))
        .thenReturn(Optional.of(admin));
    when(jwtService.generateAccessToken(admin)).thenReturn("admin-access-token");
    when(jwtService.generateRefreshToken(admin)).thenReturn("admin-refresh-token");
    when(jwtService.getAccessTokenTtlSeconds()).thenReturn(900L);
    when(refreshTokenRepository.save(any(RefreshToken.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    AuthResponse response = authService.switchBackToAdmin("Bearer impersonated-access-token");

    assertEquals("admin-access-token", response.accessToken());
    assertEquals("admin-refresh-token", response.refreshToken());
    assertEquals("ROLE_ADMIN", response.role());
  }

  @Test
  void switchBackToAdminShouldRejectNonImpersonatedSession() {
    when(jwtService.isValid("plain-access-token")).thenReturn(true);
    when(jwtService.extractImpersonatedBy("plain-access-token")).thenReturn(null);

    assertThrows(
        BadRequestException.class,
        () -> authService.switchBackToAdmin("Bearer plain-access-token"));
  }
}
