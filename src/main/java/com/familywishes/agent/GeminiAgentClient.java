package com.familywishes.agent;

import com.familywishes.ai.GeminiClient;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Agent-facing handle on Gemini; model fallback and retries live in {@link GeminiClient}. */
@Component
@RequiredArgsConstructor
public class GeminiAgentClient {
  private final GeminiClient geminiClient;

  public Map<String, Object> generate(Map<String, Object> body) {
    return geminiClient.generateContent(body);
  }
}
