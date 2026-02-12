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
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    private final AuthenticationManager authManager;
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final OtpCodeRepository otpCodeRepository;
    private final PasswordResetTokenRepository resetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailServiceImpl emailService;

    @Override
    public AuthResponse login(LoginRequest request) {
        authManager.authenticate(new UsernamePasswordAuthenticationToken(request.email(), request.password()));
        User user = userRepository.findByEmailAndDeletedFalse(request.email()).orElseThrow(() -> new NotFoundException("User not found"));
        String access = jwtService.generateAccessToken(user);
        String refresh = jwtService.generateRefreshToken(user);
        refreshTokenRepository.save(RefreshToken.builder().token(refresh).user(user).expiresAt(LocalDateTime.now().plusDays(7)).revoked(false).build());
        return new AuthResponse(access, refresh, "Bearer");
    }

    @Override
    public AuthResponse refresh(RefreshRequest request) {
        var rt = refreshTokenRepository.findByTokenAndRevokedFalse(request.refreshToken()).orElseThrow(() -> new BadRequestException("Invalid refresh token"));
        if (rt.getExpiresAt().isBefore(LocalDateTime.now())) throw new BadRequestException("Refresh token expired");
        String access = jwtService.generateAccessToken(rt.getUser());
        return new AuthResponse(access, rt.getToken(), "Bearer");
    }

    @Override
    public void sendOtp(OtpSendRequest request) {
        String otp = String.format("%06d", new SecureRandom().nextInt(1_000_000));
        otpCodeRepository.save(OtpCode.builder().email(request.email()).code(otp).expiresAt(LocalDateTime.now().plusMinutes(5)).used(false).build());
        emailService.sendHtmlEmail(request.email(), "Your OTP", "<p>Your OTP is <b>" + otp + "</b>. Valid for 5 minutes.</p>", null);
    }

    @Override
    public AuthResponse verifyOtp(OtpVerifyRequest request) {
        var otp = otpCodeRepository.findTopByEmailOrderByIdDesc(request.email()).orElseThrow(() -> new BadRequestException("OTP not found"));
        if (otp.isUsed() || otp.getExpiresAt().isBefore(LocalDateTime.now()) || !otp.getCode().equals(request.otp())) {
            throw new BadRequestException("Invalid or expired OTP");
        }
        otp.setUsed(true);
        otpCodeRepository.save(otp);
        User user = userRepository.findByEmailAndDeletedFalse(request.email()).orElseThrow(() -> new NotFoundException("User not found"));
        String access = jwtService.generateAccessToken(user);
        String refresh = jwtService.generateRefreshToken(user);
        refreshTokenRepository.save(RefreshToken.builder().token(refresh).user(user).expiresAt(LocalDateTime.now().plusDays(7)).revoked(false).build());
        return new AuthResponse(access, refresh, "Bearer");
    }

    @Override
    public void forgotPassword(ForgotPasswordRequest request) {
        User user = userRepository.findByEmailAndDeletedFalse(request.email()).orElseThrow(() -> new NotFoundException("User not found"));
        String token = UUID.randomUUID().toString();
        resetTokenRepository.save(PasswordResetToken.builder().token(token).user(user).expiresAt(LocalDateTime.now().plusMinutes(30)).used(false).build());
        emailService.sendHtmlEmail(user.getEmail(), "Reset your password", "<a href='https://family-wishes/reset?token=" + token + "'>Reset Password</a>", null);
    }

    @Override
    public void resetPassword(ResetPasswordRequest request) {
        var token = resetTokenRepository.findByToken(request.token()).orElseThrow(() -> new BadRequestException("Invalid token"));
        if (token.isUsed() || token.getExpiresAt().isBefore(LocalDateTime.now())) throw new BadRequestException("Expired token");
        token.setUsed(true);
        User user = token.getUser();
        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
        resetTokenRepository.save(token);
    }
}
