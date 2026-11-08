package com.familywishes.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.familywishes.chat.ChatMessageRepository;
import com.familywishes.dto.AuthDtos.AdminLoginAsUserRequest;
import com.familywishes.dto.AuthDtos.AuthResponse;
import com.familywishes.dto.AuthDtos.ForgotPasswordRequest;
import com.familywishes.dto.AuthDtos.LoginRequest;
import com.familywishes.dto.AuthDtos.OtpSendRequest;
import com.familywishes.entity.OtpCode;
import com.familywishes.entity.PasswordResetToken;
import com.familywishes.entity.RefreshToken;
import com.familywishes.entity.User;
import com.familywishes.entity.enums.Role;
import com.familywishes.exception.BadRequestException;
import com.familywishes.exception.NotFoundException;
import com.familywishes.repository.LoginLocationEventRepository;
import com.familywishes.repository.OtpCodeRepository;
import com.familywishes.repository.PasswordResetTokenRepository;
import com.familywishes.repository.RefreshTokenRepository;
import com.familywishes.repository.UserRepository;
import com.familywishes.repository.ViolatedUserRepository;
import com.familywishes.security.JwtService;
import com.familywishes.service.GmailEmailService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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
import org.springframework.test.util.ReflectionTestUtils;

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
  @Mock private ChatMessageRepository chatMessageRepository;
  @Mock private LoginLocationEventRepository loginLocationEventRepository;
  @Mock private ViolatedUserRepository violatedUserRepository;
  @Mock private ViolatedUserAuditService violatedUserAuditService;

  @InjectMocks private AuthServiceImpl authService;

  @BeforeEach
  void setUp() {
    ReflectionTestUtils.setField(authService, "schedulerTimeZone", "UTC");
    ReflectionTestUtils.setField(authService, "failedLoginAttemptThreshold", 5);
    ReflectionTestUtils.setField(authService, "otpTtlMinutes", 5);
    ReflectionTestUtils.setField(authService, "otpMaxResendAttempts", 5);
    ReflectionTestUtils.setField(authService, "passwordResetTtlMinutes", 30);
    ReflectionTestUtils.setField(authService, "passwordResetMaxAttempts", 5);
  }

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

    when(userRepository.findByEmailAndDeletedFalse("user@example.com"))
        .thenReturn(Optional.of(user));
    when(authManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
        .thenThrow(new BadCredentialsException("bad credentials"));

    assertThrows(
        BadRequestException.class,
        () -> authService.login(new LoginRequest("user@example.com", "wrong")));

    assertEquals(2, user.getFailedLoginAttempts());
    verify(userRepository).save(user);
    verify(emailService, never())
        .sendEmailWithAttachments(
            any(String.class), any(String.class), any(String.class), any(), any());
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

    when(userRepository.findByEmailAndDeletedFalse("user@example.com"))
        .thenReturn(Optional.of(user));
    when(authManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
        .thenThrow(new BadCredentialsException("bad credentials"));

    BadRequestException exception =
        assertThrows(
            BadRequestException.class,
            () -> authService.login(new LoginRequest("user@example.com", "wrong")));

    assertEquals(
        "Too many failed login attempts. Please contact the Administrator.",
        exception.getMessage());
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

    when(userRepository.findByEmailAndDeletedFalse("user@example.com"))
        .thenReturn(Optional.of(user));

    BadRequestException exception =
        assertThrows(
            BadRequestException.class,
            () -> authService.login(new LoginRequest("user@example.com", "password")));

    assertEquals(
        "Too many failed login attempts. Please contact the Administrator.",
        exception.getMessage());
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

  @Test
  void sendOtpShouldFailWhenUserDoesNotExist() {
    when(userRepository.findByEmailAndDeletedFalse("missing@example.com"))
        .thenReturn(Optional.empty());

    assertThrows(
        NotFoundException.class,
        () -> authService.sendOtp(new OtpSendRequest("missing@example.com")));

    verify(otpCodeRepository, never()).save(any());
    verify(emailService, never())
        .sendEmailWithAttachments(
            any(String.class), any(String.class), any(String.class), any(), any());
  }

  @Test
  void sendOtpShouldSaveOtpAndSendMailForActiveUser() {
    User user = User.builder().id(7L).email("user@example.com").active(true).deleted(false).build();
    when(userRepository.findByEmailAndDeletedFalse("user@example.com"))
        .thenReturn(Optional.of(user));

    authService.sendOtp(new OtpSendRequest("user@example.com"));

    verify(otpCodeRepository).save(any());
    verify(emailService)
        .sendEmailWithAttachments(
            org.mockito.ArgumentMatchers.eq("user@example.com"),
            any(String.class),
            any(String.class),
            any(),
            any());
  }

  @Test
  void sendOtpShouldReuseTheActiveCodeInsteadOfGeneratingANewOne() {
    User user = User.builder().id(7L).email("user@example.com").active(true).deleted(false).build();
    OtpCode active =
        OtpCode.builder()
            .id(1L)
            .user(user)
            .code("123456")
            .expiresAt(java.time.LocalDateTime.now().plusMinutes(3))
            .used(false)
            .build();
    when(userRepository.findByEmailAndDeletedFalse("user@example.com"))
        .thenReturn(Optional.of(user));
    when(otpCodeRepository.findTopByUserIdAndUsedFalseAndExpiresAtAfterOrderByIdDesc(
            org.mockito.ArgumentMatchers.eq(7L), any()))
        .thenReturn(Optional.of(active));

    authService.sendOtp(new OtpSendRequest("user@example.com"));

    org.junit.jupiter.api.Assertions.assertEquals("123456", active.getCode());
    org.junit.jupiter.api.Assertions.assertEquals(1, active.getResendCount());
    verify(otpCodeRepository).save(active);
  }

  @Test
  void sendOtpShouldRejectWhenResendLimitIsReached() {
    User user = User.builder().id(7L).email("user@example.com").active(true).deleted(false).build();
    OtpCode active =
        OtpCode.builder()
            .id(1L)
            .user(user)
            .code("123456")
            .expiresAt(java.time.LocalDateTime.now().plusMinutes(3))
            .used(false)
            .resendCount(5)
            .build();
    when(userRepository.findByEmailAndDeletedFalse("user@example.com"))
        .thenReturn(Optional.of(user));
    when(otpCodeRepository.findTopByUserIdAndUsedFalseAndExpiresAtAfterOrderByIdDesc(
            org.mockito.ArgumentMatchers.eq(7L), any()))
        .thenReturn(Optional.of(active));

    assertThrows(
        BadRequestException.class,
        () -> authService.sendOtp(new OtpSendRequest("user@example.com")));
    verify(emailService, never())
        .sendEmailWithAttachments(
            any(String.class), any(String.class), any(String.class), any(), any());
  }

  @Test
  void forgotPasswordShouldReuseTheActiveToken() {
    User user = User.builder().id(9L).email("user@example.com").active(true).deleted(false).build();
    PasswordResetToken active =
        PasswordResetToken.builder()
            .id(2L)
            .token("existing-token")
            .user(user)
            .expiresAt(java.time.LocalDateTime.now().plusMinutes(20))
            .used(false)
            .build();
    when(userRepository.findByEmailAndDeletedFalse("user@example.com"))
        .thenReturn(Optional.of(user));
    when(resetTokenRepository.findTopByUserIdAndUsedFalseAndExpiresAtAfterOrderByIdDesc(
            org.mockito.ArgumentMatchers.eq(9L), any()))
        .thenReturn(Optional.of(active));

    authService.forgotPassword(new ForgotPasswordRequest("user@example.com"));

    org.junit.jupiter.api.Assertions.assertEquals("existing-token", active.getToken());
    org.junit.jupiter.api.Assertions.assertEquals(1, active.getResendCount());
    verify(resetTokenRepository).save(active);
  }

  @Test
  void forgotPasswordShouldRejectWhenAttemptLimitIsReached() {
    User user = User.builder().id(9L).email("user@example.com").active(true).deleted(false).build();
    PasswordResetToken active =
        PasswordResetToken.builder()
            .id(2L)
            .token("existing-token")
            .user(user)
            .expiresAt(java.time.LocalDateTime.now().plusMinutes(20))
            .used(false)
            .resendCount(5)
            .build();
    when(userRepository.findByEmailAndDeletedFalse("user@example.com"))
        .thenReturn(Optional.of(user));
    when(resetTokenRepository.findTopByUserIdAndUsedFalseAndExpiresAtAfterOrderByIdDesc(
            org.mockito.ArgumentMatchers.eq(9L), any()))
        .thenReturn(Optional.of(active));

    assertThrows(
        BadRequestException.class,
        () -> authService.forgotPassword(new ForgotPasswordRequest("user@example.com")));
    verify(emailService, never())
        .sendEmailWithAttachments(
            any(String.class), any(String.class), any(String.class), any(), any());
  }

  @Test
  void loginShouldRequireOtpWhenMfaIsEnabled() {
    User user =
        User.builder()
            .id(11L)
            .email("mfa@example.com")
            .password("encoded")
            .role(Role.ROLE_USER)
            .mfaEnabled(true)
            .build();
    when(userRepository.findByEmailAndDeletedFalse("mfa@example.com"))
        .thenReturn(Optional.of(user));

    BadRequestException ex =
        assertThrows(
            BadRequestException.class,
            () -> authService.login(new LoginRequest("mfa@example.com", "correct")));
    org.junit.jupiter.api.Assertions.assertEquals("MFA_OTP_REQUIRED", ex.getMessage());
    verify(refreshTokenRepository, never()).save(any());
  }

  @Test
  void loginShouldRevokeEarlierSessionsWhenConcurrentLoginIsDisallowed() {
    User user =
        User.builder()
            .id(21L)
            .email("single@example.com")
            .password("encoded")
            .role(Role.ROLE_USER)
            .active(true)
            .concurrentLoginAllowed(false)
            .build();

    when(userRepository.findByEmailAndDeletedFalse("single@example.com"))
        .thenReturn(Optional.of(user));
    when(authManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
        .thenReturn(new UsernamePasswordAuthenticationToken("single@example.com", "correct"));
    when(jwtService.generateAccessToken(user, "single@example.com")).thenReturn("access-token");
    when(jwtService.generateRefreshToken(user, "single@example.com")).thenReturn("refresh-token");
    when(jwtService.getAccessTokenTtlSeconds()).thenReturn(900L);
    when(refreshTokenRepository.save(any(RefreshToken.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    when(chatMessageRepository.countUnreadMessagesForReceiver(21L)).thenReturn(0L);

    authService.login(new LoginRequest("single@example.com", "correct"));

    verify(refreshTokenRepository).revokeAllByUserId(21L);
  }

  @Test
  void loginShouldKeepEarlierSessionsWhenConcurrentLoginIsAllowed() {
    User user =
        User.builder()
            .id(22L)
            .email("multi@example.com")
            .password("encoded")
            .role(Role.ROLE_USER)
            .active(true)
            .concurrentLoginAllowed(true)
            .build();

    when(userRepository.findByEmailAndDeletedFalse("multi@example.com"))
        .thenReturn(Optional.of(user));
    when(authManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
        .thenReturn(new UsernamePasswordAuthenticationToken("multi@example.com", "correct"));
    when(jwtService.generateAccessToken(user, "multi@example.com")).thenReturn("access-token");
    when(jwtService.generateRefreshToken(user, "multi@example.com")).thenReturn("refresh-token");
    when(jwtService.getAccessTokenTtlSeconds()).thenReturn(900L);
    when(refreshTokenRepository.save(any(RefreshToken.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    when(chatMessageRepository.countUnreadMessagesForReceiver(22L)).thenReturn(0L);

    authService.login(new LoginRequest("multi@example.com", "correct"));

    verify(refreshTokenRepository, never()).revokeAllByUserId(any());
  }

  @Test
  void forgotPasswordShouldFailWhenUserIsInactive() {
    User inactiveUser =
        User.builder().id(8L).email("inactive@example.com").active(false).deleted(false).build();
    when(userRepository.findByEmailAndDeletedFalse("inactive@example.com"))
        .thenReturn(Optional.of(inactiveUser));

    assertThrows(
        NotFoundException.class,
        () -> authService.forgotPassword(new ForgotPasswordRequest("inactive@example.com")));

    verify(resetTokenRepository, never()).save(any());
    verify(emailService, never())
        .sendEmailWithAttachments(
            any(String.class), any(String.class), any(String.class), any(), any());
  }

  @Test
  void forgotPasswordShouldFailWhenUserDoesNotExist() {
    when(userRepository.findByEmailAndDeletedFalse("missing@example.com"))
        .thenReturn(Optional.empty());

    assertThrows(
        NotFoundException.class,
        () -> authService.forgotPassword(new ForgotPasswordRequest("missing@example.com")));

    verify(resetTokenRepository, never()).save(any());
    verify(emailService, never())
        .sendEmailWithAttachments(
            any(String.class), any(String.class), any(String.class), any(), any());
  }
}
