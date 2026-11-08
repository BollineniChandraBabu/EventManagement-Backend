package com.familywishes.security;

import com.familywishes.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {
  private static final long ACCESS_TOKEN_TTL_SECONDS = 15 * 60;
  private static final long REFRESH_TOKEN_TTL_SECONDS = 7 * 24 * 60 * 60;

  @Value("${app.jwt.secret}")
  private String secret;

  public String generateAccessToken(User user) {
    return generateAccessToken(user, Map.of());
  }

  public String generateAccessToken(User user, String impersonatedBy) {
    Map<String, Object> extraClaims = new HashMap<>();
    if (impersonatedBy != null && !impersonatedBy.isBlank()) {
      extraClaims.put("impersonatedBy", impersonatedBy);
    }
    return generateAccessToken(user, extraClaims);
  }

  /**
   * Adds the user's current session id as the {@code sid} claim, so a later request can tell
   * whether the token still belongs to the active session. Reads it straight from the entity, so
   * every sign-in path stamps the right session without each caller having to pass it in.
   */
  private String generateAccessToken(User user, Map<String, Object> extraClaims) {
    Map<String, Object> claims = new HashMap<>(extraClaims);
    if (user.getActiveSessionId() != null && !user.getActiveSessionId().isBlank()) {
      claims.put("sid", user.getActiveSessionId());
    }
    return generate(user.getEmail(), user.getRole().name(), ACCESS_TOKEN_TTL_SECONDS, claims);
  }

  public String generateRefreshToken(User user) {
    return generate(user.getEmail(), "REFRESH", REFRESH_TOKEN_TTL_SECONDS, Map.of());
  }

  public String generateRefreshToken(User user, String impersonatedBy) {
    Map<String, Object> extraClaims = new HashMap<>();
    if (impersonatedBy != null && !impersonatedBy.isBlank()) {
      extraClaims.put("impersonatedBy", impersonatedBy);
    }
    return generate(user.getEmail(), "REFRESH", REFRESH_TOKEN_TTL_SECONDS, extraClaims);
  }

  public long getAccessTokenTtlSeconds() {
    return ACCESS_TOKEN_TTL_SECONDS;
  }

  private String generate(String subject, String scope, long ttlSec, Map<String, Object> claims) {
    Instant now = Instant.now();
    return Jwts.builder()
        .subject(subject)
        .claim("scope", scope)
        .claims(claims)
        .issuedAt(Date.from(now))
        .expiration(Date.from(now.plusSeconds(ttlSec)))
        .signWith(getKey())
        .compact();
  }

  public String extractSubject(String token) {
    return parse(token).getSubject();
  }

  public String extractImpersonatedBy(String token) {
    return parse(token).get("impersonatedBy", String.class);
  }

  /**
   * The active-session id baked into an access token, or null for tokens issued before it existed.
   */
  public String extractSessionId(String token) {
    return parse(token).get("sid", String.class);
  }

  public boolean isValid(String token) {
    try {
      parse(token);
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  private Claims parse(String token) {
    return Jwts.parser().verifyWith(getKey()).build().parseSignedClaims(token).getPayload();
  }

  private SecretKey getKey() {
    return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
  }
}
