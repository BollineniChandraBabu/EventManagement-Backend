package com.familywishes.controller;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

  @GetMapping("/health")
  public ResponseEntity<?> health() {
    return ResponseEntity.ok(
        Map.of("status", "UP", "time", LocalDateTime.now(ZoneId.of("Asia/Kolkata"))));
  }
}
