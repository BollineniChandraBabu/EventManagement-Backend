package com.familywishes.agent;

import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/** Thin wrapper over the same Gemini endpoint the app already uses (app.gemini.api.*). */
@Component
@RequiredArgsConstructor
public class GeminiAgentClient {
  private final RestTemplate restTemplate;

  @Value("${app.gemini.api.key}")
  private String apiKey;

  @Value("${app.gemini.api.url}")
  private String apiUrl;

  @SuppressWarnings("unchecked")
  public Map<String, Object> generate(Map<String, Object> body) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    return restTemplate
        .exchange(apiUrl + apiKey, HttpMethod.POST, new HttpEntity<>(body, headers), Map.class)
        .getBody();
  }
}
