package com.familywishes.security;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.familywishes.exception.RateLimitExceededException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class RateLimiterTest {

  private RateLimiter rateLimiter;

  @BeforeEach
  void setUp() {
    rateLimiter = new RateLimiter();
    ReflectionTestUtils.setField(rateLimiter, "enabled", true);
    ReflectionTestUtils.setField(rateLimiter, "maxRequests", 3);
    ReflectionTestUtils.setField(rateLimiter, "windowSeconds", 60L);
  }

  @Test
  void allowsUpToTheLimitThenBlocks() {
    assertDoesNotThrow(() -> rateLimiter.check("ip|/api/auth/login"));
    assertDoesNotThrow(() -> rateLimiter.check("ip|/api/auth/login"));
    assertDoesNotThrow(() -> rateLimiter.check("ip|/api/auth/login"));

    assertThrows(RateLimitExceededException.class, () -> rateLimiter.check("ip|/api/auth/login"));
  }

  @Test
  void tracksEachKeyIndependently() {
    assertDoesNotThrow(() -> rateLimiter.check("ip-a|/api/auth/login"));
    assertDoesNotThrow(() -> rateLimiter.check("ip-b|/api/auth/login"));
    assertDoesNotThrow(() -> rateLimiter.check("ip-b|/api/auth/login"));
    assertDoesNotThrow(() -> rateLimiter.check("ip-b|/api/auth/login"));

    assertThrows(RateLimitExceededException.class, () -> rateLimiter.check("ip-b|/api/auth/login"));
  }

  @Test
  void isBypassedWhenDisabled() {
    ReflectionTestUtils.setField(rateLimiter, "enabled", false);

    for (int i = 0; i < 100; i++) {
      rateLimiter.check("ip|/api/auth/login");
    }
  }
}
