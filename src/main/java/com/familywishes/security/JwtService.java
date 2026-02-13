package com.familywishes.security;

import com.familywishes.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

@Service
public class JwtService {
    private static final long ACCESS_TOKEN_TTL_SECONDS = 15 * 60;
    private static final long REFRESH_TOKEN_TTL_SECONDS = 7 * 24 * 60 * 60;

    @Value("${app.jwt.secret}")
    private String secret;

    public String generateAccessToken(User user) {
        return generate(user.getEmail(), user.getRole().name(), ACCESS_TOKEN_TTL_SECONDS);
    }

    public String generateRefreshToken(User user) {
        return generate(user.getEmail(), "REFRESH", REFRESH_TOKEN_TTL_SECONDS);
    }

    public long getAccessTokenTtlSeconds() {
        return ACCESS_TOKEN_TTL_SECONDS;
    }

    private String generate(String subject, String scope, long ttlSec) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(subject)
                .claim("scope", scope)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(ttlSec)))
                .signWith(getKey())
                .compact();
    }

    public String extractSubject(String token) {
        return parse(token).getSubject();
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
