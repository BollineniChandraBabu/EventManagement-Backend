package com.familywishes.ai;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/** Stands in for Google: answers the list-models call and the generateContent call. */
class FakeGeminiRestTemplate extends RestTemplate {

  static final Map<String, Object> ANSWER = Map.of("candidates", List.of());

  /** Models Google "lists". Each one supports text generation. */
  List<String> listedModels = new ArrayList<>();

  /** Models whose generateContent call fails with the given error. */
  final Map<String, RestClientException> failures = new HashMap<>();

  /** If set, the list-models call fails with this error. */
  RestClientException listFailure;

  final List<String> generateCalls = new ArrayList<>();
  int listCalls = 0;

  @Override
  @SuppressWarnings("unchecked")
  public <T> ResponseEntity<T> exchange(
      String url,
      HttpMethod method,
      HttpEntity<?> requestEntity,
      Class<T> responseType,
      Object... uriVariables) {
    if (method == HttpMethod.GET) {
      return (ResponseEntity<T>) ResponseEntity.ok(listModels());
    }
    String model = url.substring(url.indexOf("/models/") + 8, url.indexOf(":generateContent"));
    generateCalls.add(model);
    RestClientException failure = failures.get(model);
    if (failure != null) {
      throw failure;
    }
    return (ResponseEntity<T>) ResponseEntity.ok(ANSWER);
  }

  private GeminiModelCatalog.ModelList listModels() {
    listCalls++;
    if (listFailure != null) {
      throw listFailure;
    }
    List<GeminiModelCatalog.ModelInfo> models = new ArrayList<>();
    for (String id : listedModels) {
      models.add(new GeminiModelCatalog.ModelInfo("models/" + id, List.of("generateContent")));
    }
    models.add(new GeminiModelCatalog.ModelInfo("models/not-for-text", List.of("embedContent")));
    return new GeminiModelCatalog.ModelList(models);
  }
}
