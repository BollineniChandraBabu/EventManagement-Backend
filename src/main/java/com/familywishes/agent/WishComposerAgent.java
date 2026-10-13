package com.familywishes.agent;

import com.familywishes.dto.AiWishRequest;
import com.familywishes.dto.AiWishResponse;
import com.familywishes.entity.EmailLog;
import com.familywishes.entity.enums.EmailStatus;
import com.familywishes.repository.EmailLogRepository;
import com.familywishes.repository.UserRepository;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Agentic wish writer. Instead of one blind prompt it: reads the recipient's profile, checks what
 * was already sent to them, drafts a fresh message, and must pass a validator tool
 * (submit_wish) before the draft is accepted. Rejections are fed back so the model self-corrects.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WishComposerAgent {
  private static final Pattern PLACEHOLDER =
      Pattern.compile("\\[[A-Za-z _]+]|\\{\\{.*?}}|\\{[A-Za-z_]+}|<name>|%s|\\bundefined\\b");

  private final AgentRunner runner;
  private final UserRepository userRepository;
  private final EmailLogRepository emailLogRepository;

  @Value("${app.agent.sender-name:Chandra}")
  private String senderName;

  @Value("${app.agent.wish.max-steps:6}")
  private int maxSteps;

  public Optional<AiWishResponse> compose(AiWishRequest request) {
    Long userId = request.userId();

    List<AgentTool> tools = new ArrayList<>();
    if (userId != null) {
      tools.add(profileTool(userId));
      tools.add(recentWishesTool(userId));
    }
    tools.add(submitTool(request, userId));

    String occasion = firstNonBlank(request.event(), request.festival());
    String system =
        """
        You write warm, personal greeting emails for the Golden Greetings family app, sent on \
        behalf of %s.
        Workflow:
        1. If tools are available, call get_recipient_profile and get_recent_wishes first.
        2. Write a message that feels fresh: do not reuse subjects or phrasing from recent wishes.
        3. Call submit_wish with the final subject and htmlMessage. If it is rejected, fix every \
        listed problem and submit again.
        Rules: use the exact recipient name, never placeholders, simple inline HTML only \
        (<p>, <br>, <b>, <i>), no markdown, no scripts. Match the requested tone and language.
        You must finish by calling submit_wish.
        """
            .formatted(senderName);

    String user =
        """
        Write a personalized %s wish email.
        Recipient name: %s
        Sender name: %s
        Relationship: %s
        Festival: %s
        Tone: %s
        Language: %s
        """
            .formatted(
                occasion,
                request.name(),
                senderName,
                request.relation(),
                nullToEmpty(request.festival()),
                request.tone(),
                request.language());

    AgentResult result =
        runner.run(new AgentSpec(system, user, tools, maxSteps, Set.of("submit_wish"), true));
    log.info("WishComposerAgent steps={} trace={}", result.steps(), result.trace());

    if (!result.completed() || result.terminalArgs() == null) {
      return Optional.empty();
    }
    Map<String, Object> args = result.terminalArgs();
    return Optional.of(
        new AiWishResponse(
            String.valueOf(args.get("subject")).trim(),
            String.valueOf(args.get("htmlMessage")).trim()));
  }

  private AgentTool profileTool(Long userId) {
    return AgentTool.of(
        "get_recipient_profile",
        "Get non-sensitive profile details of the recipient (name, relationship, birthday month/day).",
        null,
        args ->
            userRepository
                .findById(userId)
                .<Object>map(
                    u -> {
                      Map<String, Object> m = new HashMap<>();
                      m.put("name", u.getName());
                      m.put(
                          "relationship",
                          u.getRelationShip() == null ? null : u.getRelationShip().getDisplayName());
                      m.put("gender", u.getGender() == null ? null : u.getGender().name());
                      if (u.getBirthday() != null) {
                        m.put("birthdayMonth", u.getBirthday().getMonth().toString());
                        m.put("birthdayDay", u.getBirthday().getDayOfMonth());
                      }
                      return m;
                    })
                .orElse(Map.of("error", "user not found")));
  }

  private AgentTool recentWishesTool(Long userId) {
    return AgentTool.of(
        "get_recent_wishes",
        "Get up to 5 wishes recently sent to this recipient (subject and a short text preview) so you can avoid repeating them.",
        null,
        args -> Map.of("wishes", recentWishes(userId)));
  }

  private List<Map<String, Object>> recentWishes(Long userId) {
    List<Map<String, Object>> out = new ArrayList<>();
    for (EmailLog l :
        emailLogRepository.findTop5ByRecipientUserIdAndStatusOrderBySentAtDesc(
            userId, EmailStatus.SENT)) {
      Map<String, Object> m = new HashMap<>();
      m.put("subject", l.getSubject());
      m.put("type", String.valueOf(l.getEmailType()));
      m.put("preview", preview(l.getBody()));
      out.add(m);
    }
    return out;
  }

  private AgentTool submitTool(AiWishRequest request, Long userId) {
    return AgentTool.of(
        "submit_wish",
        "Submit the final wish. Returns accepted=true, or a list of problems to fix before resubmitting.",
        ToolSchema.create()
            .string("subject", "Email subject line, max 120 characters", true)
            .string("htmlMessage", "Email body as simple inline HTML", true)
            .build(),
        args -> {
          String subject = String.valueOf(args.getOrDefault("subject", "")).trim();
          String html = String.valueOf(args.getOrDefault("htmlMessage", "")).trim();
          List<String> problems = new ArrayList<>();

          if (subject.isEmpty()) problems.add("subject is empty");
          if (subject.length() > 120) problems.add("subject longer than 120 characters");
          if (html.length() < 20) problems.add("htmlMessage is too short");
          if (html.contains("```")) problems.add("remove markdown code fences");
          if (html.toLowerCase(Locale.ROOT).contains("<script")) problems.add("no <script> allowed");
          if (PLACEHOLDER.matcher(subject + " " + html).find()) {
            problems.add("contains a placeholder-like token; use real values");
          }
          String name = request.name() == null ? "" : request.name().trim().toLowerCase(Locale.ROOT);
          if (!name.isEmpty()
              && !(subject + " " + html).toLowerCase(Locale.ROOT).contains(name)) {
            problems.add("recipient name '" + request.name() + "' must appear in the message");
          }
          if (userId != null) {
            for (EmailLog l : recentLogs(userId)) {
              if (l.getSubject() != null && l.getSubject().equalsIgnoreCase(subject)) {
                problems.add("subject duplicates a recently sent wish; write a different one");
                break;
              }
            }
          }
          if (problems.isEmpty()) {
            return Map.of("accepted", true);
          }
          return Map.of("accepted", false, "problems", problems);
        });
  }

  private List<EmailLog> recentLogs(Long userId) {
    return emailLogRepository.findTop5ByRecipientUserIdAndStatusOrderBySentAtDesc(
        userId, EmailStatus.SENT);
  }

  private static String preview(String html) {
    if (html == null) return "";
    String text = html.replaceAll("<[^>]+>", " ").replaceAll("\\s+", " ").trim();
    return text.length() > 200 ? text.substring(0, 200) + "..." : text;
  }

  private static String firstNonBlank(String a, String b) {
    return a != null && !a.isBlank() ? a : nullToEmpty(b);
  }

  private static String nullToEmpty(String s) {
    return s == null ? "" : s;
  }
}
