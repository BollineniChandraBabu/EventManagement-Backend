package com.familywishes.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * In-memory sliding window rate limiter targeting sensitive authentication endpoints to prevent
 * brute force attacks: - /api/auth/login - /api/auth/otp/verify - /api/auth/otp/verify-login -
 * /api/auth/forgot-password - /api/auth/password-reset/confirm - /api/auth/otp/send
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 50)
@Slf4j
public class RateLimitingFilter extends OncePerRequestFilter {

  private static final int WINDOW_SECONDS = 60;

  @Value("${app.security.rate-limit.login-per-minute:10}")
  private int maxLoginRequestsPerMinute;

  @Value("${app.security.rate-limit.otp-per-minute:10}")
  private int maxOtpRequestsPerMinute;

  @Value("${app.security.rate-limit.forgot-password-per-minute:5}")
  private int maxForgotPasswordRequestsPerMinute;

  private final Map<String, RequestCounter> requestCounts = new ConcurrentHashMap<>();

  private static class RequestCounter {
    final long windowStart;
    final AtomicInteger count;

    RequestCounter(long windowStart) {
      this.windowStart = windowStart;
      this.count = new AtomicInteger(1);
    }
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    String uri = request.getRequestURI();
    String method = request.getMethod();

    if ("POST".equalsIgnoreCase(method)) {
      Integer limit = resolveLimit(uri);
      if (limit != null) {
        String clientIp = resolveClientIp(request);
        String key = clientIp + ":" + uri;

        if (isRateLimited(key, limit)) {
          log.warn("Rate limit exceeded for IP: {} on URI: {}", clientIp, uri);
          response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
          response.setContentType(MediaType.APPLICATION_JSON_VALUE);
          response
              .getWriter()
              .write(
                  "{\"error\":\"Too Many Requests\",\"message\":\"Rate limit exceeded. Please try again later.\"}");
          return;
        }
      }
    }

    filterChain.doFilter(request, response);
  }

  private Integer resolveLimit(String uri) {
    if (uri.endsWith("/api/auth/login")) {
      return maxLoginRequestsPerMinute;
    }
    if (uri.endsWith("/api/auth/otp/verify")
        || uri.endsWith("/api/auth/otp/verify-login")
        || uri.endsWith("/api/auth/otp/send")) {
      return maxOtpRequestsPerMinute;
    }
    if (uri.endsWith("/api/auth/forgot-password")
        || uri.endsWith("/api/auth/password-reset/confirm")) {
      return maxForgotPasswordRequestsPerMinute;
    }
    return null;
  }

  private boolean isRateLimited(String key, int maxRequests) {
    long now = System.currentTimeMillis();
    long currentWindow = now / (WINDOW_SECONDS * 1000L);
    String fullKey = key + ":" + currentWindow;

    RequestCounter counter =
        requestCounts.compute(
            fullKey,
            (k, v) -> {
              if (v == null) {
                return new RequestCounter(now);
              }
              v.count.incrementAndGet();
              return v;
            });

    if (requestCounts.size() > 5000) {
      requestCounts
          .entrySet()
          .removeIf(entry -> (now - entry.getValue().windowStart) > (WINDOW_SECONDS * 2000L));
    }

    return counter.count.get() > maxRequests;
  }

  private String resolveClientIp(HttpServletRequest request) {
    String forwardedFor = request.getHeader("X-Forwarded-For");
    if (forwardedFor != null && !forwardedFor.isBlank()) {
      return forwardedFor.split(",")[0].trim();
    }
    String realIp = request.getHeader("X-Real-IP");
    if (realIp != null && !realIp.isBlank()) {
      return realIp.trim();
    }
    return request.getRemoteAddr() != null ? request.getRemoteAddr() : "unknown";
  }
}
