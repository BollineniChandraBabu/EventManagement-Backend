package com.familywishes.controller;

import com.familywishes.dto.AiWishRequest;
import com.familywishes.dto.AiWishResponse;
import com.familywishes.service.AiService;
import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.validation.Valid;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {
  private final AiService aiService;

  @GetMapping("/pollinations/balance")
  public Map<String, Object> getPollinationsBalance() {
    return aiService.getPollinationsBalance();
  }

  @PostMapping("/generate-wish")
  public AiWishResponse generate(@Valid @RequestBody AiWishRequest request)
      throws JsonProcessingException {
    return aiService.generate(request);
  }
}
