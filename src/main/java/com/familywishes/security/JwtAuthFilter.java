package com.familywishes.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {
  /** Marker the UI looks for in the response body to show the concurrent-login notice. */
  private static final String CONCURRENT_LOGIN_CODE = "CONCURRENT_LOGIN";

  private final JwtService jwtService;
  private final UserDetailsService userDetailsService;

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    String auth = request.getHeader("Authorization");
    if (auth == null || !auth.startsWith("Bearer ")) {
      filterChain.doFilter(request, response);
      return;
    }
    String token = auth.substring(7);
    if (!jwtService.isValid(token)) {
      filterChain.doFilter(request, response);
      return;
    }
    String email = jwtService.extractSubject(token);
    var details = userDetailsService.loadUserByUsername(email);

    if (details instanceof AppUserDetails appUser
        && !appUser.isConcurrentLoginAllowed()
        && appUser.getActiveSessionId() != null
        && !appUser.getActiveSessionId().equals(jwtService.extractSessionId(token))) {
      writeConcurrentLogin(response);
      return;
    }

    var authentication =
        new UsernamePasswordAuthenticationToken(details, null, details.getAuthorities());
    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
    SecurityContextHolder.getContext().setAuthentication(authentication);
    filterChain.doFilter(request, response);
  }

  /**
   * Rejects a request whose token belongs to a session that a newer single-session sign-in has
   * replaced. Written here rather than by a controller advice because the filter runs before the
   * DispatcherServlet. A 401 keeps the client's normal auth handling, and the body carries the
   * {@code CONCURRENT_LOGIN} marker so the UI can explain why and offer a fresh sign-in.
   */
  private void writeConcurrentLogin(HttpServletResponse response) throws IOException {
    response.setStatus(HttpStatus.UNAUTHORIZED.value());
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response.setCharacterEncoding("UTF-8");
    response
        .getWriter()
        .write(
            "{\"timestamp\":\""
                + LocalDateTime.now()
                + "\",\"status\":401,\"message\":\""
                + CONCURRENT_LOGIN_CODE
                + ": Your account was signed in on another device, so this session ended. Please sign in again.\"}");
  }
}
