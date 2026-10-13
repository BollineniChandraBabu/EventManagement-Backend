package com.familywishes.controller;

import com.familywishes.agent.AdminCopilotAgent;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/agent")
@RequiredArgsConstructor
public class AgentController {
  private final AdminCopilotAgent adminCopilot;

  @Value("${app.agent.copilot.enabled:false}")
  private boolean copilotEnabled;

  public record AskRequest(@NotBlank @Size(max = 500) String question) {}

  @PreAuthorize("hasRole('ADMIN')")
  @PostMapping("/admin/ask")
  public ResponseEntity<Map<String, String>> ask(@Valid @RequestBody AskRequest request) {
    if (!copilotEnabled) {
      return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
          .body(Map.of("answer", "Admin copilot is disabled (app.agent.copilot.enabled=false)."));
    }
    return ResponseEntity.ok(Map.of("answer", adminCopilot.ask(request.question())));
  }
}
