package com.familywishes.security;

import com.familywishes.exception.RateLimitExceededException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Applies {@link RateLimiter} to the sensitive, unauthenticated auth endpoints (login, OTP, SSO,
 * password reset). The limit is per client IP and per endpoint, so one attacker cannot exhaust
 * another user's budget.
 */
@Component
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

  private static final Set<String> LIMITED_PATHS =
      Set.of(
          "/api/auth/login",
          "/api/auth/refresh",
          "/api/auth/otp/send",
          "/api/auth/otp/verify",
          "/api/auth/otp/verify-login",
          "/api/auth/forgot-password",
          "/api/auth/password-reset/confirm",
          "/api/auth/change-password",
          "/api/auth/sso/google");

  private final RateLimiter rateLimiter;

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    String path = request.getServletPath();
    if (LIMITED_PATHS.contains(path)) {
      try {
        rateLimiter.check(clientIp(request) + "|" + path);
      } catch (RateLimitExceededException ex) {
        writeTooManyRequests(response, ex.getMessage());
        return;
      }
    }
    filterChain.doFilter(request, response);
  }

  /**
   * Filters run before the DispatcherServlet, so the 429 is written here, not by a controller
   * advice.
   */
  private void writeTooManyRequests(HttpServletResponse response, String message)
      throws IOException {
    response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response.setCharacterEncoding("UTF-8");
    response
        .getWriter()
        .write(
            "{\"timestamp\":\""
                + LocalDateTime.now()
                + "\",\"status\":429,\"message\":\""
                + message.replace("\"", "'")
                + "\"}");
  }

  private String clientIp(HttpServletRequest request) {
    String forwardedFor = request.getHeader("X-Forwarded-For");
    if (forwardedFor != null && !forwardedFor.isBlank()) {
      return forwardedFor.split(",")[0].trim();
    }
    String realIp = request.getHeader("X-Real-IP");
    if (realIp != null && !realIp.isBlank()) {
      return realIp.trim();
    }
    String remote = request.getRemoteAddr();
    return remote == null ? "unknown" : remote;
  }
}
