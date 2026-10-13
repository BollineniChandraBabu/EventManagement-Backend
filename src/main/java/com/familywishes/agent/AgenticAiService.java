package com.familywishes.agent;

import com.familywishes.dto.AiWishRequest;
import com.familywishes.dto.AiWishResponse;
import com.familywishes.service.AiService;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

/**
 * Drop-in replacement for AiService (marked @Primary) so every existing caller - birthday, festival,
 * good-morning/night schedulers, UserServiceImpl, AiController - gets agentic wish generation
 * without code changes. Falls back to the original single-prompt implementation on any failure.
 * Enable with app.agent.wish.enabled=true.
 */
@Service
@Primary
@Slf4j
public class AgenticAiService implements AiService {
  private final AiService delegate;
  private final WishComposerAgent composer;
  private final boolean enabled;

  public AgenticAiService(
      @Qualifier("aiServiceImpl") AiService delegate,
      WishComposerAgent composer,
      @Value("${app.agent.wish.enabled:false}") boolean enabled) {
    this.delegate = delegate;
    this.composer = composer;
    this.enabled = enabled;
  }

  @Override
  public AiWishResponse generate(AiWishRequest request) throws JsonProcessingException {
    if (enabled) {
      try {
        var result = composer.compose(request);
        if (result.isPresent()) {
          return result.get();
        }
        log.warn("WishComposerAgent produced no accepted wish; falling back to single prompt");
      } catch (Exception e) {
        log.warn("WishComposerAgent failed ({}); falling back to single prompt", e.getMessage());
      }
    }
    return delegate.generate(request);
  }

  @Override
  public byte[] callGeminiImage(AiWishRequest request) throws JsonProcessingException {
    return delegate.callGeminiImage(request);
  }

  @Override
  public Map<String, Object> getPollinationsBalance() {
    return delegate.getPollinationsBalance();
  }
}
