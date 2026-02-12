package com.familywishes.service;

import com.familywishes.dto.AuthDtos.*;

public interface AuthService {
    AuthResponse login(LoginRequest request);
    AuthResponse refresh(RefreshRequest request);
    void sendOtp(OtpSendRequest request);
    AuthResponse verifyOtp(OtpVerifyRequest request);
    void forgotPassword(ForgotPasswordRequest request);
    void resetPassword(ResetPasswordRequest request);
}
