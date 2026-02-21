package com.familywishes.service.impl;

import com.familywishes.dto.AuthDtos.*;
import com.familywishes.entity.OtpCode;
import com.familywishes.entity.PasswordResetToken;
import com.familywishes.entity.RefreshToken;
import com.familywishes.entity.User;
import com.familywishes.exception.BadRequestException;
import com.familywishes.exception.NotFoundException;
import com.familywishes.repository.*;
import com.familywishes.security.JwtService;
import com.familywishes.service.AuthService;
import com.familywishes.service.GmailEmailService;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
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

  @Value("${app.password-reset.base-url:https://family-wishes/reset}")
  private String passwordResetBaseUrl;

  @Value("${app.password-reset.ttl-minutes:30}")
  private int passwordResetTtlMinutes;

  @Override
  public AuthResponse login(LoginRequest request) {
    authManager.authenticate(
        new UsernamePasswordAuthenticationToken(request.email(), request.password()));
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
    return new AuthResponse(
        access, refresh, "Bearer", user.getRole().name(), jwtService.getAccessTokenTtlSeconds());
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
        jwtService.getAccessTokenTtlSeconds());
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
        request.email(),
        "Your Family Wishes OTP",
        buildOtpEmailBody(otp),
        null,
        null);
  }

  private String buildOtpEmailBody(String otp) {
    return
        """
            <div style='font-family:Arial,sans-serif;line-height:1.5;color:#111827'>
              <h3 style='margin-bottom:8px'>Family Wishes Verification Code</h3>
              <p style='margin:0 0 12px 0'>Use the OTP below to complete your sign-in.</p>
              <p style='margin:0 0 12px 0'>
                <span style='display:inline-block;padding:10px 16px;border:1px solid #d1d5db;border-radius:8px;font-size:22px;font-weight:700;letter-spacing:4px'>%s</span>
              </p>
              <p style='margin:0 0 8px 0'>This OTP is valid for <b>%d minutes</b>.</p>
              <p style='margin:0;color:#6b7280'>If you did not request this code, please ignore this email.</p>
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
    return new AuthResponse(
        access, refresh, "Bearer", user.getRole().name(), jwtService.getAccessTokenTtlSeconds());
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

    String resetUrl = passwordResetBaseUrl + "?token=" + token + "&email=" + user.getEmail();
    String body =
        """
                <div style='font-family:Arial,sans-serif;line-height:1.5'>
                  <h3>Password Reset Request</h3>
                  <p>Hi %s,</p>
                  <p>We received a request to reset your Family Wishes password.</p>
                  <p><a href='%s'>Click here to reset your password</a></p>
                  <p>If the button doesn't work, use this token in the reset API:</p>
                  <p><b>%s</b></p>
                  <p>This link/token expires in %d minutes.</p>
                  <p>If you did not request this, you can safely ignore this email.</p>
                </div>
                """
            .formatted(user.getName(), resetUrl, token, passwordResetTtlMinutes);

    emailService.sendEmailWithAttachments(
        user.getEmail(), "Reset your Family Wishes password", body, null, null);
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
}
