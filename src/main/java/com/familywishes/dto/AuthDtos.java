package com.familywishes.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class AuthDtos {
  public record LoginRequest(
      @Email String email,
      @NotBlank String password,
      String loginLocation,
      String ipAddress,
      Double latitude,
      Double longitude) {

    /** Email/password only; the optional location fields default to empty. */
    public LoginRequest(String email, String password) {
      this(email, password, null, null, null, null);
    }
  }

  public record AdminLoginAsUserRequest(@Email String email) {}

  public record AuthResponse(
      String accessToken,
      String refreshToken,
      String tokenType,
      String role,
      long expiresIn,
      long unreadChatMessagesCount) {}

  public record AuthSSOClientResponse(String clientId) {}

  public record RefreshRequest(@NotBlank String refreshToken) {}

  public record OtpSendRequest(@Email String email) {}

  public record OtpVerifyRequest(@Email String email, @NotBlank String otp) {}

  public record LoginOtpVerifyRequest(
      @Email String email,
      @NotBlank String otp,
      String loginLocation,
      String ipAddress,
      Double latitude,
      Double longitude) {}

  public record ForgotPasswordRequest(@Email String email) {}

  public record ResetPasswordRequest(@NotBlank String token, @NotBlank String newPassword) {}

  public record ResetPasswordWithEmailRequest(
      @Email String email, @NotBlank String token, @NotBlank String newPassword) {}

  public record ChangePasswordRequest(
      @NotBlank String currentPassword, @NotBlank String newPassword) {}

  public record GoogleSsoLoginRequest(@NotBlank String idToken) {}

  public record DirectLoginRequest(
      @NotBlank String token,
      @Email String email,
      String loginLocation,
      String ipAddress,
      Double latitude,
      Double longitude) {

    /** Token only; the optional email and location fields default to empty. */
    public DirectLoginRequest(String token) {
      this(token, null, null, null, null, null);
    }
  }
}
