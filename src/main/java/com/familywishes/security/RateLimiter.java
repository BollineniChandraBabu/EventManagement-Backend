package com.familywishes.security;

import com.familywishes.exception.RateLimitExceededException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * A small in-memory sliding-window limiter for the sensitive auth endpoints (login, OTP, password
 * reset). It is intentionally per-instance: it protects a single node from brute-force bursts
 * without adding a shared store. The map is bounded and pruned so it cannot grow without limit.
 */
@Component
@Slf4j
public class RateLimiter {

  private static final int MAX_TRACKED_KEYS = 100_000;

  private final Map<String, Deque<Instant>> hits = new ConcurrentHashMap<>();

  @Value("${app.rate-limit.enabled:true}")
  private boolean enabled;

  @Value("${app.rate-limit.max-requests:10}")
  private int maxRequests;

  @Value("${app.rate-limit.window-seconds:60}")
  private long windowSeconds;

  /** Records one hit for {@code key} and throws when the window limit is exceeded. */
  public void check(String key) {
    if (!enabled) {
      return;
    }
    Instant now = Instant.now();
    Instant cutoff = now.minus(Duration.ofSeconds(windowSeconds));

    if (hits.size() > MAX_TRACKED_KEYS) {
      prune(cutoff);
    }

    Deque<Instant> window =
        hits.compute(
            key,
            (k, existing) -> {
              Deque<Instant> deque = existing == null ? new ArrayDeque<>() : existing;
              while (!deque.isEmpty() && deque.peekFirst().isBefore(cutoff)) {
                deque.pollFirst();
              }
              deque.addLast(now);
              return deque;
            });

    if (window.size() > maxRequests) {
      throw new RateLimitExceededException("Too many attempts. Please try again in a minute.");
    }
  }

  private void prune(Instant cutoff) {
    hits.entrySet()
        .removeIf(
            entry -> {
              Deque<Instant> window = entry.getValue();
              synchronized (window) {
                while (!window.isEmpty() && window.peekFirst().isBefore(cutoff)) {
                  window.pollFirst();
                }
                return window.isEmpty();
              }
            });
  }
}
