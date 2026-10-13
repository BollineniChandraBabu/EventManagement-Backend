package com.familywishes.agent;

import com.familywishes.entity.ViolatedUser;
import com.familywishes.entity.enums.EmailStatus;
import com.familywishes.repository.EmailLogRepository;
import com.familywishes.repository.EventRepository;
import com.familywishes.repository.UserRepository;
import com.familywishes.repository.ViolatedUserRepository;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Read-only natural-language assistant for admins ("how many emails failed today?", "any suspicious
 * logins this week?", "whose birthdays are in March?"). Has no write tools by design.
 */
@Component
@RequiredArgsConstructor
public class AdminCopilotAgent {
  private final AgentRunner runner;
  private final EmailLogRepository emailLogRepository;
  private final ViolatedUserRepository violatedUserRepository;
  private final UserRepository userRepository;
  private final EventRepository eventRepository;

  @Value("${scheduler.time-zone:Asia/Kolkata}")
  private String zone;

  public String ask(String question) {
    List<AgentTool> tools =
        List.of(
            AgentTool.of(
                "get_email_counts",
                "Total email counts by status (SENT, FAILED, PENDING).",
                null,
                a -> {
                  Map<String, Object> m = new LinkedHashMap<>();
                  for (EmailStatus s : EmailStatus.values()) {
                    m.put(s.name(), emailLogRepository.countByStatus(s));
                  }
                  return m;
                }),
            AgentTool.of(
                "get_failed_logins",
                "Failed login attempts in the last N hours (max 168), grouped by email with count, locations and IPs. Passwords are never included.",
                ToolSchema.create().integer("hours", "Look-back window in hours", true).build(),
                a -> failedLogins(a)),
            AgentTool.of(
                "get_upcoming_birthdays",
                "Names and birthday day-of-month for users whose birthday is in the given month (1-12).",
                ToolSchema.create().integer("month", "Month number 1-12", true).build(),
                a -> birthdays(a)),
            AgentTool.of(
                "get_events_in_month",
                "Count of registered events in the given month (1-12).",
                ToolSchema.create().integer("month", "Month number 1-12", true).build(),
                a -> {
                  Integer month = monthArg(a);
                  return month == null
                      ? Map.of("error", "month must be 1-12")
                      : Map.of("count", eventRepository.findByMonth(month).size());
                }),
            AgentTool.of(
                "get_user_count",
                "Number of active (non-deleted) users.",
                null,
                a -> Map.of("users", userRepository.countByDeletedFalse())));

    String system =
        """
        You are a read-only operations assistant for the administrators of Golden Greetings.
        Answer using the tools; never guess numbers. If the tools cannot answer, say so.
        Be concise. Never reveal passwords, tokens or secrets, and never speculate about a \
        person's guilt - report counts and patterns only.
        """;

    AgentResult r = runner.run(new AgentSpec(system, question, tools, 6, Set.of(), false));
    return r.completed() && r.finalText() != null && !r.finalText().isBlank()
        ? r.finalText()
        : "I couldn't complete that request. Try rephrasing or narrowing the question.";
  }

  private Object failedLogins(Map<String, Object> a) {
    int hours = a.get("hours") instanceof Number n ? Math.max(1, Math.min(168, n.intValue())) : 24;
    LocalDateTime end = LocalDateTime.now(ZoneId.of(zone));
    List<ViolatedUser> rows =
        violatedUserRepository.findByLoggedInAtGreaterThanEqualAndLoggedInAtLessThanEqual(
            end.minusHours(hours), end);

    Map<String, Map<String, Object>> byEmail = new LinkedHashMap<>();
    for (ViolatedUser v : rows) {
      Map<String, Object> g = byEmail.computeIfAbsent(v.getEmail(), k -> newGroup());
      g.put("attempts", (int) g.get("attempts") + 1);
      addDistinct(g, "locations", v.getLoginLocation());
      addDistinct(g, "ips", v.getIpAddress());
    }
    List<Map<String, Object>> out = new ArrayList<>();
    byEmail.forEach((email, g) -> {
      g.put("email", email);
      out.add(g);
    });
    out.sort((x, y) -> (int) y.get("attempts") - (int) x.get("attempts"));
    return Map.of("windowHours", hours, "totalAttempts", rows.size(),
        "accounts", out.size() > 20 ? out.subList(0, 20) : out);
  }

  private Object birthdays(Map<String, Object> a) {
    Integer month = monthArg(a);
    if (month == null) return Map.of("error", "month must be 1-12");
    List<Map<String, Object>> out = new ArrayList<>();
    userRepository.findByMonth(month).forEach(u -> {
      Map<String, Object> m = new HashMap<>();
      m.put("name", u.getName());
      m.put("day", u.getBirthday().getDayOfMonth());
      out.add(m);
    });
    return Map.of("birthdays", out);
  }

  private static Map<String, Object> newGroup() {
    Map<String, Object> g = new LinkedHashMap<>();
    g.put("attempts", 0);
    g.put("locations", new ArrayList<String>());
    g.put("ips", new ArrayList<String>());
    return g;
  }

  @SuppressWarnings("unchecked")
  private static void addDistinct(Map<String, Object> g, String key, String value) {
    List<String> list = (List<String>) g.get(key);
    if (value != null && !list.contains(value) && list.size() < 5) list.add(value);
  }

  private static Integer monthArg(Map<String, Object> a) {
    if (a.get("month") instanceof Number n && n.intValue() >= 1 && n.intValue() <= 12) {
      return n.intValue();
    }
    return null;
  }
}
