package com.familywishes.controller;

import com.familywishes.agent.AdminCopilotAgent;
import com.familywishes.agent.DeliveryOpsAgent;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/agent")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AgentController {
  private final AdminCopilotAgent adminCopilot;
  private final DeliveryOpsAgent deliveryOps;

  private final AtomicBoolean deliveryOpsRunning = new AtomicBoolean(false);

  @Value("${app.agent.copilot.enabled:false}")
  private boolean copilotEnabled;

  @Value("${app.agent.wish.enabled:false}")
  private boolean wishEnabled;

  @Value("${app.agent.delivery-ops.enabled:false}")
  private boolean deliveryOpsEnabled;

  @Value("${app.agent.delivery-ops.dry-run:true}")
  private boolean deliveryOpsDryRun;

  public record AskRequest(@NotBlank @Size(max = 500) String question) {}

  @GetMapping("/status")
  public Map<String, Object> status() {
    Map<String, Object> ops = new LinkedHashMap<>();
    ops.put("enabled", deliveryOpsEnabled);
    ops.put("dryRun", deliveryOpsDryRun);
    ops.put("failedEmails", deliveryOps.failedCount());

    Map<String, Object> out = new LinkedHashMap<>();
    out.put("wishComposer", Map.of("enabled", wishEnabled));
    out.put("deliveryOps", ops);
    out.put("copilot", Map.of("enabled", copilotEnabled));
    return out;
  }

  @PostMapping("/admin/ask")
  public ResponseEntity<Map<String, String>> ask(@Valid @RequestBody AskRequest request) {
    if (!copilotEnabled) {
      return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
          .body(Map.of("answer", "Admin copilot is disabled (app.agent.copilot.enabled=false)."));
    }
    return ResponseEntity.ok(Map.of("answer", adminCopilot.ask(request.question())));
  }

  /** Manual triage run. Same guardrails as the scheduled run (dry-run flag, retry/alert budgets). */
  @PostMapping("/delivery-ops/run")
  public ResponseEntity<Map<String, Object>> runDeliveryOps() {
    Map<String, Object> out = new LinkedHashMap<>();
    out.put("dryRun", deliveryOpsDryRun);

    if (!deliveryOpsEnabled) {
      out.put("ran", false);
      out.put(
          "report", "Delivery ops agent is disabled (app.agent.delivery-ops.enabled=false).");
      return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(out);
    }
    if (!deliveryOpsRunning.compareAndSet(false, true)) {
      out.put("ran", false);
      out.put("report", "A triage run is already in progress.");
      return ResponseEntity.status(HttpStatus.CONFLICT).body(out);
    }
    try {
      if (deliveryOps.failedCount() == 0) {
        out.put("ran", false);
        out.put("report", "No failed emails to triage.");
        return ResponseEntity.ok(out);
      }
      String report = deliveryOps.run();
      out.put("ran", true);
      out.put("report", report == null || report.isBlank() ? "Run finished without a report." : report);
      return ResponseEntity.ok(out);
    } finally {
      deliveryOpsRunning.set(false);
    }
  }
}
