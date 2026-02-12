package com.familywishes.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class AuthDtos {
    public record LoginRequest(@Email String email, @NotBlank String password) {}
    public record AuthResponse(String accessToken, String refreshToken, String tokenType) {}
    public record RefreshRequest(@NotBlank String refreshToken) {}
    public record OtpSendRequest(@Email String email) {}
    public record OtpVerifyRequest(@Email String email, @NotBlank String otp) {}
    public record ForgotPasswordRequest(@Email String email) {}
    public record ResetPasswordRequest(@NotBlank String token, @NotBlank String newPassword) {}
}
