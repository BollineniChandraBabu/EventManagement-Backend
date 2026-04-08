package com.familywishes.service;

import com.familywishes.dto.AuthDtos.*;

public interface AuthService {
  AuthResponse login(LoginRequest request);

  AuthResponse adminLoginAsUser(AdminLoginAsUserRequest request);

  AuthResponse switchBackToAdmin(String authorizationHeader);

  AuthResponse refresh(RefreshRequest request);

  void sendOtp(OtpSendRequest request);

  AuthResponse verifyOtp(OtpVerifyRequest request);

  void forgotPassword(ForgotPasswordRequest request);

  void resetPassword(ResetPasswordRequest request);

  void resetPasswordWithEmail(ResetPasswordWithEmailRequest request);

  void changePassword(ChangePasswordRequest request);

  AuthResponse googleSsoLogin(GoogleSsoLoginRequest request);
}
