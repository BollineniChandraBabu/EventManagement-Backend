package com.familywishes.agent;

import com.familywishes.entity.EmailLog;
import com.familywishes.entity.enums.EmailStatus;
import com.familywishes.entity.enums.EmailType;
import com.familywishes.repository.EmailLogRepository;
import com.familywishes.service.GmailEmailService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Triage agent for failed emails. Reads failures, classifies the error (transient vs auth vs
 * permanent), retries only what is safe, and sends at most one admin alert with a concrete fix.
 * Runs in dry-run mode by default: actions are described, not executed.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DeliveryOpsAgent {
  private static final Set<EmailType> NEVER_RETRY =
      Set.of(EmailType.OTP, EmailType.FORGOT_PASSWORD); // stale codes are useless and confusing

  private final AgentRunner runner;
  private final EmailLogRepository emailLogRepository;
  private final GmailEmailService gmailEmailService;

  @Value("${alert.email.to}")
  private String alertEmail;

  @Value("${app.agent.delivery-ops.dry-run:true}")
  private boolean dryRun;

  @Value("${app.agent.delivery-ops.max-retries-per-run:10}")
  private int maxRetriesPerRun;

  public long failedCount() {
    return emailLogRepository.countByStatus(EmailStatus.FAILED);
  }

  public String run() {
    AtomicInteger retries = new AtomicInteger();
    AtomicInteger alerts = new AtomicInteger();

    List<AgentTool> tools =
        List.of(
            AgentTool.of(
                "list_failed_emails",
                "List the 20 most recent FAILED emails with id, type, retryCount and error message.",
                null,
                a -> Map.of("failed", failedEmails())),
            AgentTool.of(
                "retry_email",
                "Retry sending one failed email by log id. Only for transient errors. OTP and password-reset emails are refused.",
                ToolSchema.create().integer("logId", "EmailLog id to retry", true).build(),
                a -> retry(a, retries)),
            AgentTool.of(
                "send_admin_alert",
                "Email the administrator. Max one alert per run. Use for auth/config problems or repeated permanent failures.",
                ToolSchema.create()
                    .string("subject", "Short alert subject", true)
                    .string("body", "What failed, likely cause, and the exact fix", true)
                    .build(),
                a -> alert(a, alerts)));

    String system =
        """
        You are the delivery-operations agent for the Golden Greetings email pipeline.
        Call list_failed_emails, then classify each error:
        - transient (timeouts, 5xx, rate limits, connection reset) -> retry_email if retryCount < 3
        - auth/config (401/403, invalid_grant, expired token, missing credentials) -> do NOT retry; \
        send_admin_alert explaining the likely cause and fix
        - permanent (invalid recipient, bad address, content rejected) -> do NOT retry; mention in report
        Never retry the same id twice. Finish with a 3-5 line plain-text report of what you did and why.
        """;

    AgentResult result =
        runner.run(
            new AgentSpec(
                system,
                "Triage the current failed emails now.",
                tools,
                8,
                Set.of(),
                false));
    log.info("DeliveryOpsAgent dryRun={} trace={}", dryRun, result.trace());
    return result.finalText();
  }

  private List<Map<String, Object>> failedEmails() {
    List<Map<String, Object>> out = new ArrayList<>();
    for (EmailLog l : emailLogRepository.findTop20ByStatusOrderByIdDesc(EmailStatus.FAILED)) {
      Map<String, Object> m = new HashMap<>();
      m.put("id", l.getId());
      m.put("type", String.valueOf(l.getEmailType()));
      m.put("retryCount", l.getRetryCount());
      String err = l.getErrorMessage();
      m.put("error", err == null ? "" : err.length() > 300 ? err.substring(0, 300) : err);
      out.add(m); // recipient address deliberately not exposed to the model
    }
    return out;
  }

  private Object retry(Map<String, Object> args, AtomicInteger retries) {
    Long id = toLong(args.get("logId"));
    if (id == null) return Map.of("error", "logId required");
    if (retries.get() >= maxRetriesPerRun) return Map.of("error", "retry budget for this run used up");

    EmailLog l = emailLogRepository.findWithRecipientById(id).orElse(null);
    if (l == null) return Map.of("error", "email log not found");
    if (l.getStatus() != EmailStatus.FAILED) return Map.of("error", "email is not in FAILED state");
    if (l.getRetryCount() >= 3) return Map.of("error", "retry limit (3) already reached");
    if (l.getEmailType() != null && NEVER_RETRY.contains(l.getEmailType())) {
      return Map.of("error", "this email type must not be retried");
    }
    retries.incrementAndGet();
    if (dryRun) return Map.of("dryRun", true, "wouldRetry", id);

    gmailEmailService.sendEmailWithAttachments(
        l.getRecipientEmail(), l.getSubject(), l.getBody(), l.getId(), null);
    return Map.of("retryQueued", id);
  }

  private Object alert(Map<String, Object> args, AtomicInteger alerts) {
    if (alerts.get() >= 1) return Map.of("error", "alert limit for this run reached");
    String subject = String.valueOf(args.getOrDefault("subject", "Delivery alert"));
    String body = String.valueOf(args.getOrDefault("body", ""));
    alerts.incrementAndGet();
    if (dryRun) return Map.of("dryRun", true, "wouldAlert", subject);

    String html = "<p>" + body.replace("&", "&amp;").replace("<", "&lt;").replace("\n", "<br>") + "</p>";
    gmailEmailService.sendEmailWithAttachments(
        alertEmail, "🤖 " + subject, html, null, null);
    return Map.of("alertSent", true);
  }

  private static Long toLong(Object o) {
    return o instanceof Number n ? n.longValue() : null;
  }
}
