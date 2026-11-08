package com.familywishes.service;

import com.familywishes.dto.AuthDtos.*;
import jakarta.servlet.http.HttpServletRequest;

public interface AuthService {
  AuthResponse login(LoginRequest request, HttpServletRequest httpRequest);

  AuthResponse adminLoginAsUser(AdminLoginAsUserRequest request);

  AuthResponse switchBackToAdmin(String authorizationHeader);

  AuthResponse refresh(RefreshRequest request);

  void sendOtp(OtpSendRequest request);

  AuthResponse verifyOtp(OtpVerifyRequest request);

  AuthResponse verifyLoginOtp(LoginOtpVerifyRequest request, HttpServletRequest httpRequest);

  void forgotPassword(ForgotPasswordRequest request);

  void resetPassword(ResetPasswordRequest request);

  void resetPasswordWithEmail(ResetPasswordWithEmailRequest request);

  void changePassword(ChangePasswordRequest request);

  AuthResponse googleSsoLogin(GoogleSsoLoginRequest request);

  AuthSSOClientResponse getSSOAuthToken();

  /**
   * Creates (or reuses a still-valid) single-use token that lets the user sign in from an emailed
   * link. Returns the raw token to embed in the link; {@code null} for an unknown user.
   */
  String issueDirectLoginToken(Long userId);

  /** Signs a user in from a single-use emailed link token, consuming it. */
  AuthResponse directLogin(DirectLoginRequest request, HttpServletRequest httpRequest);
}
