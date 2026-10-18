package com.familywishes.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.familywishes.exception.AiUnavailableException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

/** Model names here are made-up fixtures; the production code contains no model names. */
class GeminiClientTest {

  private static final String API_URL =
      "https://api.example.test/v1beta/models/primary:generateContent";
  private static final int COOLDOWN_SECONDS = 60;

  private final FakeGeminiRestTemplate google = new FakeGeminiRestTemplate();

  private GeminiClient client(int maxRetries, long cooldownSeconds) {
    GeminiModelCatalog catalog = new GeminiModelCatalog(google, API_URL, "key", 60, "");
    return new GeminiClient(google, catalog, API_URL, "key", maxRetries, cooldownSeconds);
  }

  private static HttpClientErrorException rateLimited() {
    return new HttpClientErrorException(HttpStatus.TOO_MANY_REQUESTS);
  }

  @Test
  void usesThePrimaryModelWhenItWorks() {
    Map<String, Object> answer = client(5, COOLDOWN_SECONDS).generateContent(Map.of());

    assertEquals(FakeGeminiRestTemplate.ANSWER, answer);
    assertEquals(List.of("primary"), google.generateCalls);
    assertEquals(0, google.listCalls);
  }

  @Test
  void triesTheNextListedModelWhenRateLimited() {
    google.listedModels = List.of("primary", "other-1", "other-2");
    google.failures.put("primary", rateLimited());

    client(5, COOLDOWN_SECONDS).generateContent(Map.of());

    assertEquals(List.of("primary", "other-1"), google.generateCalls);
  }

  @Test
  void skipsAModelThatJustFailed() {
    google.listedModels = List.of("primary", "other-1");
    google.failures.put("primary", rateLimited());
    GeminiClient client = client(5, COOLDOWN_SECONDS);
    client.generateContent(Map.of());
    google.generateCalls.clear();

    client.generateContent(Map.of());

    assertEquals(List.of("other-1"), google.generateCalls);
  }

  @Test
  void triesAFailedModelAgainAfterTheCooldown() {
    google.listedModels = List.of("primary", "other-1");
    google.failures.put("primary", rateLimited());
    GeminiClient client = client(5, 0); // 0 seconds: no cooldown
    client.generateContent(Map.of());
    google.generateCalls.clear();

    client.generateContent(Map.of());

    assertEquals(List.of("primary", "other-1"), google.generateCalls);
  }

  @Test
  void stopsAfterTheConfiguredNumberOfRetries() {
    google.listedModels = List.of("m1", "m2", "m3", "m4", "m5", "m6", "m7", "m8");
    google.failures.put("primary", rateLimited());
    google.listedModels.forEach(model -> google.failures.put(model, rateLimited()));

    GeminiClient client = client(2, COOLDOWN_SECONDS);

    assertThrows(AiUnavailableException.class, () -> client.generateContent(Map.of()));

    assertEquals(3, google.generateCalls.size()); // first call + 2 retries
  }

  @Test
  void failsWhenNoModelIsLeftToTry() {
    google.listedModels = List.of("primary");
    google.failures.put("primary", rateLimited());

    GeminiClient client = client(5, COOLDOWN_SECONDS);

    assertThrows(AiUnavailableException.class, () -> client.generateContent(Map.of()));

    assertEquals(List.of("primary"), google.generateCalls);
  }

  @Test
  void movesOnForMissingModelsServerErrorsAndNetworkErrors() {
    List<org.springframework.web.client.RestClientException> errors =
        List.of(
            new HttpClientErrorException(HttpStatus.NOT_FOUND),
            new HttpServerErrorException(HttpStatus.SERVICE_UNAVAILABLE),
            new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR),
            new ResourceAccessException("timeout"));
    for (var error : errors) {
      google.generateCalls.clear();
      google.listedModels = List.of("primary", "other-1");
      google.failures.put("primary", error);

      client(5, COOLDOWN_SECONDS).generateContent(Map.of());

      assertEquals(List.of("primary", "other-1"), google.generateCalls);
    }
  }

  @Test
  void doesNotRetryErrorsAnotherModelCannotFix() {
    List<HttpStatus> statuses =
        List.of(HttpStatus.BAD_REQUEST, HttpStatus.UNAUTHORIZED, HttpStatus.FORBIDDEN);
    for (HttpStatus status : statuses) {
      google.generateCalls.clear();
      google.listedModels = List.of("primary", "other-1");
      google.failures.put("primary", new HttpClientErrorException(status));

      GeminiClient client = client(5, COOLDOWN_SECONDS);

      assertThrows(HttpClientErrorException.class, () -> client.generateContent(Map.of()));

      assertEquals(List.of("primary"), google.generateCalls);
    }
  }
}
