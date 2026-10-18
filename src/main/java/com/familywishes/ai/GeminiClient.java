package com.familywishes.ai;

import com.familywishes.exception.AiUnavailableException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * Calls Gemini's generateContent. If a model is rate limited or unavailable, the next available
 * model is tried, up to {@code app.gemini.fallback.max-retries} extra times.
 *
 * <p>Order of models: the one in {@code app.gemini.api.url} first, then the ones listed by {@link
 * GeminiModelCatalog}. A model that just failed is skipped for a short cooldown.
 */
@Component
@Slf4j
public class GeminiClient {

  private static final String API_KEY_HEADER = "x-goog-api-key";

  private final RestTemplate restTemplate;
  private final GeminiModelCatalog modelCatalog;
  private final String apiKey;
  private final String baseUrl;
  private final String primaryModel;
  private final int maxRetries;
  private final Duration cooldown;
  private final Map<String, Instant> unavailableUntil = new ConcurrentHashMap<>();

  public GeminiClient(
      RestTemplate restTemplate,
      GeminiModelCatalog modelCatalog,
      @Value("${app.gemini.api.url}") String apiUrl,
      @Value("${app.gemini.api.key}") String apiKey,
      @Value("${app.gemini.fallback.max-retries:5}") int maxRetries,
      @Value("${app.gemini.fallback.cooldown-seconds:60}") long cooldownSeconds) {
    GeminiEndpoint endpoint = GeminiEndpoint.parse(apiUrl);
    this.restTemplate = restTemplate;
    this.modelCatalog = modelCatalog;
    this.apiKey = apiKey;
    this.baseUrl = endpoint.baseUrl();
    this.primaryModel = endpoint.model();
    this.maxRetries = maxRetries;
    this.cooldown = Duration.ofSeconds(cooldownSeconds);
  }

  public Map<String, Object> generateContent(Map<String, Object> requestBody) {
    List<String> triedModels = new ArrayList<>();
    RestClientException lastError = null;

    for (int attempt = 0; attempt <= maxRetries; attempt++) {
      Optional<String> model = pickNextModel(triedModels);
      if (model.isEmpty()) {
        break;
      }
      triedModels.add(model.get());
      try {
        return callModel(model.get(), requestBody);
      } catch (RestClientException e) {
        if (!isRetryable(e)) {
          throw e;
        }
        markUnavailable(model.get());
        lastError = e;
        log.warn("Gemini model {} failed ({}), trying another model", model.get(), describe(e));
      }
    }
    throw new AiUnavailableException("No Gemini model answered. Tried: " + triedModels, lastError);
  }

  private Optional<String> pickNextModel(List<String> triedModels) {
    if (triedModels.isEmpty() && isAvailable(primaryModel)) {
      return Optional.of(primaryModel);
    }
    return modelCatalog.availableModels().stream()
        .filter(model -> !triedModels.contains(model))
        .filter(this::isAvailable)
        .findFirst();
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> callModel(String model, Map<String, Object> requestBody) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.set(API_KEY_HEADER, apiKey);
    Map<String, Object> response =
        restTemplate
            .exchange(
                baseUrl + "/models/" + model + ":generateContent",
                HttpMethod.POST,
                new HttpEntity<>(requestBody, headers),
                Map.class)
            .getBody();
    return response == null ? Map.of() : response;
  }

  /** Rate limit, model gone, server error or network problem. Other errors (bad key) are not. */
  private static boolean isRetryable(RestClientException e) {
    if (e instanceof HttpStatusCodeException httpError) {
      int status = httpError.getStatusCode().value();
      return status == HttpStatus.TOO_MANY_REQUESTS.value()
          || status == HttpStatus.NOT_FOUND.value()
          || httpError.getStatusCode().is5xxServerError();
    }
    return e instanceof ResourceAccessException;
  }

  private void markUnavailable(String model) {
    unavailableUntil.put(model, Instant.now().plus(cooldown));
  }

  private boolean isAvailable(String model) {
    Instant until = unavailableUntil.get(model);
    return until == null || !until.isAfter(Instant.now());
  }

  private static String describe(RestClientException e) {
    return e instanceof HttpStatusCodeException httpError
        ? "HTTP " + httpError.getStatusCode().value()
        : "network error";
  }
}
