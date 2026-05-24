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
}
