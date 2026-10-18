package com.familywishes.ai;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * Asks Google which Gemini models can generate text and remembers the answer for a while. New
 * models show up and retired ones disappear without any code or config change.
 */
@Component
@Slf4j
public class GeminiModelCatalog {

  private static final String API_KEY_HEADER = "x-goog-api-key";
  private static final String GENERATE_CONTENT = "generateContent";
  private static final String MODEL_NAME_PREFIX = "models/";
  private static final Duration RETRY_AFTER_FAILURE = Duration.ofMinutes(1);

  private final RestTemplate restTemplate;
  private final String listModelsUrl;
  private final String apiKey;
  private final Duration cacheDuration;
  private final Pattern excludedModels;

  private List<String> cachedModels = List.of();
  private Instant cacheExpiresAt = Instant.EPOCH;

  public GeminiModelCatalog(
      RestTemplate restTemplate,
      @Value("${app.gemini.api.url}") String apiUrl,
      @Value("${app.gemini.api.key}") String apiKey,
      @Value("${app.gemini.fallback.model-cache-minutes:60}") long cacheMinutes,
      @Value("${app.gemini.fallback.excluded-model-pattern:}") String excludedModelPattern) {
    this.restTemplate = restTemplate;
    this.listModelsUrl = GeminiEndpoint.parse(apiUrl).baseUrl() + "/models?pageSize=200";
    this.apiKey = apiKey;
    this.cacheDuration = Duration.ofMinutes(cacheMinutes);
    this.excludedModels =
        excludedModelPattern.isBlank()
            ? null
            : Pattern.compile(excludedModelPattern.trim(), Pattern.CASE_INSENSITIVE);
  }

  /** Usable model ids in Google's order. Keeps the previous list if Google cannot be reached. */
  public synchronized List<String> availableModels() {
    if (Instant.now().isBefore(cacheExpiresAt)) {
      return cachedModels;
    }
    try {
      cachedModels = fetchModels();
      cacheExpiresAt = Instant.now().plus(cacheDuration);
      log.info("Gemini models available for fallback: {}", cachedModels);
    } catch (RestClientException e) {
      cacheExpiresAt = Instant.now().plus(RETRY_AFTER_FAILURE);
      log.warn("Could not list Gemini models, keeping the previous list: {}", e.getMessage());
    }
    return cachedModels;
  }

  private List<String> fetchModels() {
    HttpHeaders headers = new HttpHeaders();
    headers.set(API_KEY_HEADER, apiKey);
    ModelList response =
        restTemplate
            .exchange(listModelsUrl, HttpMethod.GET, new HttpEntity<>(headers), ModelList.class)
            .getBody();
    if (response == null || response.models() == null) {
      return List.of();
    }
    return response.models().stream()
        .filter(this::canGenerateText)
        .map(model -> removePrefix(model.name()))
        .filter(this::isNotExcluded)
        .distinct()
        .toList();
  }

  private boolean canGenerateText(ModelInfo model) {
    return model.name() != null
        && model.supportedGenerationMethods() != null
        && model.supportedGenerationMethods().contains(GENERATE_CONTENT);
  }

  private boolean isNotExcluded(String modelId) {
    return excludedModels == null || !excludedModels.matcher(modelId).find();
  }

  private static String removePrefix(String name) {
    return Objects.requireNonNull(name).startsWith(MODEL_NAME_PREFIX)
        ? name.substring(MODEL_NAME_PREFIX.length())
        : name;
  }

  /** Response of Google's list-models API (only the fields we need). */
  record ModelList(List<ModelInfo> models) {}

  record ModelInfo(String name, List<String> supportedGenerationMethods) {}
}
