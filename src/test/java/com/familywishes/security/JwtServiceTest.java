package com.familywishes.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.familywishes.entity.User;
import com.familywishes.entity.enums.Role;
import java.lang.reflect.Field;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JwtServiceTest {
  private static final String SECRET =
      "test-secret-test-secret-test-secret-test-secret-test-secret-1234567890";

  private final JwtService jwtService = new JwtService();

  @BeforeEach
  void setUp() throws Exception {
    Field secret = JwtService.class.getDeclaredField("secret");
    secret.setAccessible(true);
    secret.set(jwtService, SECRET);
  }

  private User user(String activeSessionId) {
    return User.builder()
        .id(1L)
        .email("user@example.com")
        .role(Role.ROLE_USER)
        .active(true)
        .concurrentLoginAllowed(false)
        .activeSessionId(activeSessionId)
        .build();
  }

  @Test
  void stampsTheActiveSessionIdAsTheSidClaim() {
    String token = jwtService.generateAccessToken(user("session-1"));

    assertEquals("session-1", jwtService.extractSessionId(token));
  }

  @Test
  void omitsTheSidClaimWhenThereIsNoActiveSession() {
    String token = jwtService.generateAccessToken(user(null));

    assertNull(jwtService.extractSessionId(token));
  }

  @Test
  void impersonatedTokensAlsoCarryTheSessionId() {
    User user = user("session-2");

    String token = jwtService.generateAccessToken(user, "admin@example.com");

    assertEquals("session-2", jwtService.extractSessionId(token));
    assertEquals("admin@example.com", jwtService.extractImpersonatedBy(token));
  }
}
