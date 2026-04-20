package com.familywishes.service.impl;

import com.familywishes.dto.AuthDtos.*;
import com.familywishes.chat.ChatMessageRepository;
import com.familywishes.entity.OtpCode;
import com.familywishes.entity.PasswordResetToken;
import com.familywishes.entity.RefreshToken;
import com.familywishes.entity.User;
import com.familywishes.entity.enums.Role;
import com.familywishes.exception.BadRequestException;
import com.familywishes.exception.NotFoundException;
import com.familywishes.repository.*;
import com.familywishes.security.JwtService;
import com.familywishes.service.AuthService;
import com.familywishes.service.GmailEmailService;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
  private static final SecureRandom SECURE_RANDOM = new SecureRandom();
  private static final int OTP_TTL_MINUTES = 5;

  private final AuthenticationManager authManager;
  private final UserRepository userRepository;
  private final JwtService jwtService;
  private final RefreshTokenRepository refreshTokenRepository;
  private final OtpCodeRepository otpCodeRepository;
  private final PasswordResetTokenRepository resetTokenRepository;
  private final PasswordEncoder passwordEncoder;
  private final GmailEmailService emailService;
  private final ChatMessageRepository chatMessageRepository;

  @Value("${app.password-reset.ui-url:http://localhost:4200/reset-password}")
  private String passwordResetUiUrl;

  @Value("${app.password-reset.ttl-minutes:30}")
  private int passwordResetTtlMinutes;

  @Value("${app.login.failed-attempt-threshold:5}")
  private int failedLoginAttemptThreshold;

  @Value("${app.google.sso.client-id:${gmail.client-id:}}")
  private String googleSsoClientId;

  private static final String CONTACT_ADMIN_MESSAGE =
      "Too many failed login attempts. Please contact the Administrator.";

  @Override
  @Transactional
  public AuthResponse login(LoginRequest request) {
    User user =
        userRepository
            .findByEmailAndDeletedFalse(request.email())
            .orElseThrow(() -> new NotFoundException("User not found"));

    if (user.getFailedLoginAttempts() >= failedLoginAttemptThreshold) {
      throw new BadRequestException(CONTACT_ADMIN_MESSAGE);
    }

    try {
      authManager.authenticate(
          new UsernamePasswordAuthenticationToken(request.email(), request.password()));
    } catch (BadCredentialsException ex) {
      int failedAttempts = user.getFailedLoginAttempts() + 1;
      user.setFailedLoginAttempts(failedAttempts);
      userRepository.save(user);

      if (failedAttempts >= failedLoginAttemptThreshold) {
        emailService.sendEmailWithAttachments(
            user.getEmail(),
            "Account Security Alert",
            buildFailedLoginThresholdEmailBody(),
            null,
            null);
        throw new BadRequestException(CONTACT_ADMIN_MESSAGE);
      }
      throw new BadRequestException("Invalid email or password");
    }

    if (user.getFailedLoginAttempts() > 0) {
      user.setFailedLoginAttempts(0);
      userRepository.save(user);
    }

    String access = jwtService.generateAccessToken(user, user.getEmail());
    String refresh = jwtService.generateRefreshToken(user, user.getEmail());
    refreshTokenRepository.save(
        RefreshToken.builder()
            .token(refresh)
            .user(user)
            .expiresAt(LocalDateTime.now(ZoneId.of("Asia/Kolkata")).plusDays(7))
            .revoked(false)
            .build());
    return buildAuthResponse(user, access, refresh);
  }

  private String buildFailedLoginThresholdEmailBody() {
    return """
            <div style='font-family:Arial,sans-serif;line-height:1.5;color:#111827'>
              <h3 style='margin-bottom:8px'>Account Temporarily Locked</h3>
              <p style='margin:0 0 12px 0'>We detected multiple unsuccessful login attempts on your account.</p>
              <p style='margin:0 0 12px 0'><b>Your account sign-in has been restricted for security reasons.</b></p>
              <p style='margin:0 0 12px 0'>Please contact the Administrator to restore access.</p>
              <p style='margin:0'>Thanks and regards,<br/>Golden Greetings Team</p>
            </div>
            """;
  }

  @Override
  @Transactional
  public AuthResponse adminLoginAsUser(AdminLoginAsUserRequest request) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || !authentication.isAuthenticated()) {
      throw new BadRequestException("Authenticated user not found");
    }

    User adminUser =
        userRepository
            .findByEmailAndDeletedFalse(authentication.getName())
            .orElseThrow(() -> new NotFoundException("User not found"));

    if (adminUser.getRole() != Role.ROLE_ADMIN) {
      throw new BadRequestException("Only admin can login as user");
    }

    User user =
        userRepository
            .findByEmailAndDeletedFalse(request.email())
            .orElseThrow(() -> new NotFoundException("User not found"));

    if (user.getRole() == Role.ROLE_ADMIN) {
      throw new BadRequestException("Admin cannot login as another admin");
    }
    if (!user.isActive()) {
      throw new BadRequestException("User account is inactive");
    }
    String access = jwtService.generateAccessToken(user, adminUser.getEmail());
    String refresh = jwtService.generateRefreshToken(user, adminUser.getEmail());
    refreshTokenRepository.save(
        RefreshToken.builder()
            .token(refresh)
            .user(user)
            .expiresAt(LocalDateTime.now(ZoneId.of("Asia/Kolkata")).plusDays(7))
            .revoked(false)
            .build());
    return buildAuthResponse(user, access, refresh);
  }

  @Override
  @Transactional
  public AuthResponse switchBackToAdmin(String authorizationHeader) {
    if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
      throw new BadRequestException("Missing or invalid authorization header");
    }

    String token = authorizationHeader.substring(7);
    if (!jwtService.isValid(token)) {
      throw new BadRequestException("Invalid access token");
    }

    String impersonatedBy = jwtService.extractImpersonatedBy(token);
    if (impersonatedBy == null || impersonatedBy.isBlank()) {
      throw new BadRequestException("Switch-back is allowed only for impersonated sessions");
    }

    User adminUser =
        userRepository
            .findByEmailAndDeletedFalse(impersonatedBy)
            .orElseThrow(() -> new NotFoundException("Admin user not found"));

    if (adminUser.getRole() != Role.ROLE_ADMIN) {
      throw new BadRequestException("Original account is not an admin");
    }

    if (!adminUser.isActive()) {
      throw new BadRequestException("Admin account is inactive");
    }

    String access = jwtService.generateAccessToken(adminUser);
    String refresh = jwtService.generateRefreshToken(adminUser);
    refreshTokenRepository.save(
        RefreshToken.builder()
            .token(refresh)
            .user(adminUser)
            .expiresAt(LocalDateTime.now(ZoneId.of("Asia/Kolkata")).plusDays(7))
            .revoked(false)
            .build());

    return buildAuthResponse(adminUser, access, refresh);
  }

  @Override
  public AuthResponse refresh(RefreshRequest request) {
    RefreshToken rt =
        refreshTokenRepository
            .findByTokenAndRevokedFalse(request.refreshToken())
            .orElseThrow(() -> new BadRequestException("Invalid refresh token"));
    if (rt.getExpiresAt().isBefore(LocalDateTime.now(ZoneId.of("Asia/Kolkata"))))
      throw new BadRequestException("Refresh token expired");
    String access = jwtService.generateAccessToken(rt.getUser());
    return new AuthResponse(
        access,
        rt.getToken(),
        "Bearer",
        rt.getUser().getRole().name(),
        jwtService.getAccessTokenTtlSeconds(),
        chatMessageRepository.countUnreadMessagesForReceiver(rt.getUser().getId()));
  }

  @Override
  public void sendOtp(OtpSendRequest request) {
    String otp = String.format("%06d", SECURE_RANDOM.nextInt(1_000_000));
    otpCodeRepository.save(
        OtpCode.builder()
            .email(request.email())
            .code(otp)
            .expiresAt(LocalDateTime.now(ZoneId.of("Asia/Kolkata")).plusMinutes(OTP_TTL_MINUTES))
            .used(false)
            .build());

    emailService.sendEmailWithAttachments(
        request.email(), "Your Golden Greetings OTP", buildOtpEmailBody(otp), null, null);
  }

  private String buildOtpEmailBody(String otp) {
    return """
            <div style='font-family:Arial,sans-serif;line-height:1.5;color:#111827'>
              <h3 style='margin-bottom:8px'>Golden Greetings Verification Code</h3>
              <p style='margin:0 0 12px 0'>Use the OTP below to complete your sign-in.</p>
              <p style='margin:0 0 12px 0'>
                <span style='display:inline-block;padding:10px 16px;border:1px solid #d1d5db;border-radius:8px;font-size:22px;font-weight:700;letter-spacing:4px'>%s</span>
              </p>
              <p style='margin:0 0 8px 0'>This OTP is valid for <b>%d minutes</b>.</p>
              <p style='margin:0 0 12px 0;color:#6b7280'>If you did not request this code, please ignore this email.</p>
              <p style='margin:0'>Thanks and regards,<br/>Golden Greetings Team</p>
            </div>
            """
        .formatted(otp, OTP_TTL_MINUTES);
  }

  @Override
  public AuthResponse verifyOtp(OtpVerifyRequest request) {
    var otp =
        otpCodeRepository
            .findTopByEmailOrderByIdDesc(request.email())
            .orElseThrow(() -> new BadRequestException("OTP not found"));
    if (otp.isUsed()
        || otp.getExpiresAt().isBefore(LocalDateTime.now(ZoneId.of("Asia/Kolkata")))
        || !otp.getCode().equals(request.otp())) {
      throw new BadRequestException("Invalid or expired OTP");
    }
    otp.setUsed(true);
    otpCodeRepository.save(otp);
    User user =
        userRepository
            .findByEmailAndDeletedFalse(request.email())
            .orElseThrow(() -> new NotFoundException("User not found"));
    String access = jwtService.generateAccessToken(user);
    String refresh = jwtService.generateRefreshToken(user);
    refreshTokenRepository.save(
        RefreshToken.builder()
            .token(refresh)
            .user(user)
            .expiresAt(LocalDateTime.now(ZoneId.of("Asia/Kolkata")).plusDays(7))
            .revoked(false)
            .build());
    return buildAuthResponse(user, access, refresh);
  }

  @Override
  @Transactional
  public void forgotPassword(ForgotPasswordRequest request) {
    User user =
        userRepository
            .findByEmailAndDeletedFalse(request.email())
            .orElseThrow(() -> new NotFoundException("User not found"));
    String token = UUID.randomUUID().toString();
    resetTokenRepository.invalidateAllByUserId(user.getId());
    resetTokenRepository.save(
        PasswordResetToken.builder()
            .token(token)
            .user(user)
            .expiresAt(
                LocalDateTime.now(ZoneId.of("Asia/Kolkata")).plusMinutes(passwordResetTtlMinutes))
            .used(false)
            .build());

    String resetUrl = passwordResetUiUrl + "?token=" + token + "&email=" + user.getEmail();
    String body =
        """
                <div style='font-family:Arial,sans-serif;line-height:1.5'>
                  <h3>Password Reset Request</h3>
                  <p>Hi %s,</p>
                  <p>We received a request to reset your Golden Greetings password.</p>
                  <p><a href='%s'>Click here to reset your password</a></p>
                  <p>If the button doesn't work, use this token in the reset API:</p>
                  <p><b>%s</b></p>
                  <p>This link/token expires in %d minutes.</p>
                  <p>If you did not request this, you can safely ignore this email.</p>
                  <p>Thanks and regards,<br/>Golden Greetings Team</p>
                </div>
                """
            .formatted(user.getName(), resetUrl, token, passwordResetTtlMinutes);

    emailService.sendEmailWithAttachments(
        user.getEmail(), "Reset your Golden Greetings password", body, null, null);
  }

  @Override
  @Transactional
  public void resetPassword(ResetPasswordRequest request) {
    var token =
        resetTokenRepository
            .findByToken(request.token())
            .orElseThrow(() -> new BadRequestException("Invalid token"));
    if (token.isUsed()
        || token.getExpiresAt().isBefore(LocalDateTime.now(ZoneId.of("Asia/Kolkata"))))
      throw new BadRequestException("Expired token");
    User user = token.getUser();
    user.setPassword(passwordEncoder.encode(request.newPassword()));
    userRepository.save(user);
    resetTokenRepository.invalidateAllByUserId(user.getId());
  }

  @Override
  @Transactional
  public void changePassword(ChangePasswordRequest request) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || !authentication.isAuthenticated()) {
      throw new BadRequestException("Authenticated user not found");
    }

    User user =
        userRepository
            .findByEmailAndDeletedFalse(authentication.getName())
            .orElseThrow(() -> new NotFoundException("User not found"));

    if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
      throw new BadRequestException("Current password is incorrect");
    }

    if (request.currentPassword().equals(request.newPassword())) {
      throw new BadRequestException("New password must be different from current password");
    }

    user.setPassword(passwordEncoder.encode(request.newPassword()));
    userRepository.save(user);
    resetTokenRepository.invalidateAllByUserId(user.getId());
  }

  @Override
  @Transactional
  public void resetPasswordWithEmail(ResetPasswordWithEmailRequest request) {
    PasswordResetToken resetToken =
        resetTokenRepository
            .findByTokenAndUserEmailAndUsedFalse(request.token(), request.email())
            .orElseThrow(() -> new BadRequestException("Invalid token"));

    if (resetToken.getExpiresAt().isBefore(LocalDateTime.now(ZoneId.of("Asia/Kolkata")))) {
      throw new BadRequestException("Expired token");
    }

    User user = resetToken.getUser();
    user.setPassword(passwordEncoder.encode(request.newPassword()));
    userRepository.save(user);

    resetTokenRepository.invalidateAllByUserId(user.getId());
  }

  @Override
  @Transactional
  public AuthResponse googleSsoLogin(GoogleSsoLoginRequest request) {
    if (googleSsoClientId == null || googleSsoClientId.isBlank()) {
      throw new BadRequestException("Google SSO is not configured");
    }
    String email = verifyGoogleIdTokenAndExtractEmail(request.idToken());

    User user =
        userRepository
            .findByEmailAndDeletedFalse(email)
            .orElseThrow(() -> new NotFoundException("User not found"));
    if (!user.isActive()) {
      throw new BadRequestException("User account is inactive");
    }
    if (user.getFailedLoginAttempts() > 0) {
      user.setFailedLoginAttempts(0);
      userRepository.save(user);
    }

    String access = jwtService.generateAccessToken(user, user.getEmail());
    String refresh = jwtService.generateRefreshToken(user, user.getEmail());
    refreshTokenRepository.save(
        RefreshToken.builder()
            .token(refresh)
            .user(user)
            .expiresAt(LocalDateTime.now(ZoneId.of("Asia/Kolkata")).plusDays(7))
            .revoked(false)
            .build());
    return buildAuthResponse(user, access, refresh);
  }

  private String verifyGoogleIdTokenAndExtractEmail(String idTokenValue) {
    try {
      GoogleIdTokenVerifier verifier =
          new GoogleIdTokenVerifier.Builder(
                  GoogleNetHttpTransport.newTrustedTransport(), JacksonFactory.getDefaultInstance())
              .setAudience(Collections.singletonList(googleSsoClientId))
              .build();

      GoogleIdToken idToken = verifier.verify(idTokenValue);
      if (idToken == null) {
        throw new BadRequestException("Invalid Google ID token");
      }

      GoogleIdToken.Payload payload = idToken.getPayload();
      if (!Boolean.TRUE.equals(payload.getEmailVerified())) {
        throw new BadRequestException("Google email is not verified");
      }
      String email = payload.getEmail();
      if (email == null || email.isBlank()) {
        throw new BadRequestException("Email not found in Google token");
      }
      return email;
    } catch (BadRequestException ex) {
      throw ex;
    } catch (Exception ex) {
      ex.printStackTrace();
      throw new BadRequestException("Unable to verify Google ID token");
    }
  }

  private AuthResponse buildAuthResponse(User user, String accessToken, String refreshToken) {
    return new AuthResponse(
        accessToken,
        refreshToken,
        "Bearer",
        user.getRole().name(),
        jwtService.getAccessTokenTtlSeconds(),
        chatMessageRepository.countUnreadMessagesForReceiver(user.getId()));
  }

  public AuthSSOClientResponse getSSOAuthToken(){
    return new AuthSSOClientResponse(googleSsoClientId);
  }

}
