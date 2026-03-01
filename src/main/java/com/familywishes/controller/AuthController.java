package com.familywishes.controller;

import com.familywishes.dto.AuthDtos.*;
import com.familywishes.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
  private final AuthService authService;

  @PostMapping("/login")
  public AuthResponse login(@Valid @RequestBody LoginRequest request) {
    return authService.login(request);
  }

  @PostMapping("/refresh")
  public AuthResponse refresh(@Valid @RequestBody RefreshRequest request) {
    return authService.refresh(request);
  }
  @PostMapping("/admin/login-as-user")
  @PreAuthorize("hasRole('ADMIN')")
  public AuthResponse adminLoginAsUser(@Valid @RequestBody AdminLoginAsUserRequest request) {
    return authService.adminLoginAsUser(request);
  }

  @PostMapping("/admin/switch-back")
  @PreAuthorize("isAuthenticated()")
  public AuthResponse switchBackToAdmin(
      @RequestHeader("Authorization") String authorizationHeader) {
    return authService.switchBackToAdmin(authorizationHeader);
  }

  @PostMapping("/otp/send")
  public void sendOtp(@Valid @RequestBody OtpSendRequest request) {
    authService.sendOtp(request);
  }

  @PostMapping("/otp/verify")
  public AuthResponse verifyOtp(@Valid @RequestBody OtpVerifyRequest request) {
    return authService.verifyOtp(request);
  }

  @PostMapping("/forgot-password")
  public void forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
    authService.forgotPassword(request);
  }

  @PostMapping("/password-reset/confirm")
  public void resetPasswordWithEmail(@Valid @RequestBody ResetPasswordWithEmailRequest request) {
    authService.resetPasswordWithEmail(request);
  }

  @PostMapping("/change-password")
  public void changePassword(@Valid @RequestBody ChangePasswordRequest request) {
    authService.changePassword(request);
  }
}
